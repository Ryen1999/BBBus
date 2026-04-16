package com.bbbus.contentservice.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.bbbus.contentservice.dao.content.ExportTaskDao;
import com.bbbus.contentservice.dao.content.RocketMQTransactionLogDao;
import com.bbbus.contentservice.dao.content.ShareDao;
import com.bbbus.contentservice.domain.entity.content.ExportTaskDO;
import com.bbbus.contentservice.domain.entity.content.ShareDTO;
import com.bbbus.contentservice.dto.ExportTaskVO;
import com.bbbus.contentservice.dto.ShareExportDTO;
import com.bbbus.contentservice.dto.ShareImportDTO;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.dto.messaging.RocketMQTransactionLog;
import com.bbbus.contentservice.dto.messaging.UserAddBonusMsgDTO;
import com.bbbus.contentservice.enums.AuditStatusEnum;
import com.bbbus.contentservice.es.service.ShareEsService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShareService {

    private static final int EXPORT_BATCH_SIZE = 5000;
    private static final int IMPORT_BATCH_SIZE = 2000;  // 每批插入行数，单条SQL约1MB，安全且高效
    private static final int IMPORT_MAX_FUTURES = 10;   // 最多同时挂起的批次，控制内存峰值≈10*2000行
    private static final String BIZ_TYPE_SHARE  = "share_export";
    private static final String BIZ_TYPE_IMPORT = "share_import";

    // 状态常量，避免魔法字符串散落
    private static final String STATUS_PENDING    = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE       = "DONE";
    private static final String STATUS_FAILED     = "FAILED";
    private static final String STATUS_EXPIRED    = "EXPIRED";

    @Autowired
    private ShareDao shareDao;

    @Autowired
    private ExportTaskDao exportTaskDao;   // 任务持久化 DAO，取代了之前的 ConcurrentHashMap

    @Autowired
    private RocketMQTransactionLogDao rocketMQTransactionLogDao;

    @Autowired
    private Source source;

    @Autowired
    @Qualifier("exportQueryExecutor")
    private Executor queryExecutor;

    /** ES 服务：负责全文检索和数据同步 */
    @Autowired
    private ShareEsService shareEsService;

    // ============ Step 1：创建任务（同步，毫秒级）============

    /**
     * 创建导出任务，INSERT 一条 PENDING 状态的记录。
     * @param userId 发起人（从 JWT 拿，Controller 传入）
     * @param title  查询参数
     * @return 新生成的 taskId
     */
    public String createExportTask(String userId, String title) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        // 把查询参数打成 JSON 存起来，未来想"重跑"或审计时可以还原
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("title", title);

        ExportTaskDO task = new ExportTaskDO();
        task.setId(taskId);
        task.setUserId(userId);
        task.setBizType(BIZ_TYPE_SHARE);
        task.setParams(JSON.toJSONString(paramsMap));
        task.setStatus(STATUS_PENDING);
        task.setTotalRows(0);
        task.setCreateTime(LocalDateTime.now());
        exportTaskDao.insert(task);

        log.info("导出任务已创建: taskId={}, userId={}, title={}", taskId, userId, title);
        return taskId;
    }

    // ============ Step 2：后台异步执行导出 ============

    /**
     * @Async 让这个方法运行在 exportTaskExecutor 线程池里，调用方立即返回。
     *
     * 重要：异步线程里没有 HTTP 请求上下文，拿不到 UserContext，
     *      所以 userId 不是这里取，而是由 createExportTask 写进了 DB，
     *      这里任何时候都能通过 taskId 查回来。
     */
    @Async("exportTaskExecutor")
    public void processExportAsync(String taskId, String title) {
        // 先把状态更新为 PROCESSING
        updateTaskStatus(taskId, STATUS_PROCESSING, null, null, null, null);

        try {
            // ---- 2-1. 查总数 ----
            int total = shareDao.countShareListForExport(title);
            log.info("导出任务开始处理: taskId={}, total={}", taskId, total);

            // ---- 2-2. 并行提交所有分页查询到 queryExecutor ----
            int batchCount = (total == 0) ? 0 : (total + EXPORT_BATCH_SIZE - 1) / EXPORT_BATCH_SIZE;
            List<CompletableFuture<List<ShareDTO>>> futures = new ArrayList<>(batchCount);
            for (int i = 0; i < batchCount; i++) {
                final int offset = i * EXPORT_BATCH_SIZE;
                futures.add(CompletableFuture.supplyAsync(
                        () -> shareDao.selectShareListForExport(title, offset, EXPORT_BATCH_SIZE),
                        queryExecutor
                ));
            }

            // ---- 2-3. 建本地临时文件 & 按顺序写 Excel ----
            File tempFile = File.createTempFile("share-export-" + taskId + "-", ".xlsx");
            ExcelWriter writer = EasyExcel.write(tempFile, ShareExportDTO.class).build();
            WriteSheet sheet = EasyExcel.writerSheet("share").build();
            try {
                for (int i = 0; i < futures.size(); i++) {
                    List<ShareDTO> batch = futures.get(i).get();
                    if (batch != null && !batch.isEmpty()) {
                        writer.write(convertToExportDTO(batch), sheet);
                    }
                    log.info("导出进度: taskId={}, batch={}/{}", taskId, i + 1, batchCount);
                }
            } finally {
                writer.finish();
            }

            // ---- 2-4. 标记完成，写回 DB ----
            updateTaskStatus(taskId, STATUS_DONE,
                    total, tempFile.getAbsolutePath(), null, LocalDateTime.now());
            log.info("导出完成: taskId={}, totalRows={}, file={}",
                    taskId, total, tempFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("导出失败: taskId={}", taskId, e);
            // 把失败原因写回 DB（截断防止超长）
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.length() > 900) errMsg = errMsg.substring(0, 900);
            updateTaskStatus(taskId, STATUS_FAILED, null, null, errMsg, LocalDateTime.now());
        }
    }

    /**
     * 封装一次"选择性更新"，避免 null 值把 DB 原有数据覆盖。
     * 对应 Mapper 的 <set> 动态 SQL。
     */
    private void updateTaskStatus(String taskId, String status, Integer totalRows,
                                  String filePath, String errorMsg, LocalDateTime finishTime) {
        ExportTaskDO update = new ExportTaskDO();
        update.setId(taskId);
        update.setStatus(status);
        update.setTotalRows(totalRows);
        update.setFilePath(filePath);
        update.setErrorMsg(errorMsg);
        update.setFinishTime(finishTime);
        exportTaskDao.updateStatus(update);
    }

    // ============ Step 3a：查询任务状态 ============

    public ExportTaskVO getExportTaskStatus(String taskId, String currentUserId) {
        ExportTaskDO task = exportTaskDao.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("导出任务不存在: " + taskId);
        }
        // 越权防护：只能看自己的任务
        if (!task.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权访问他人导出任务");
        }
        return toVO(task);
    }

    /** 我的导出记录（最近 N 条） */
    public List<ExportTaskVO> listMyExports(String userId, int limit) {
        return exportTaskDao.selectByUser(userId, BIZ_TYPE_SHARE, limit)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    // ============ 导入：三步走（与导出完全对称）============

    /** Step 1：创建导入任务，立即返回 taskId */
    public String createImportTask(String userId, String originalFilename) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> params = new HashMap<>();
        params.put("filename", originalFilename);

        ExportTaskDO task = new ExportTaskDO();
        task.setId(taskId);
        task.setUserId(userId);
        task.setBizType(BIZ_TYPE_IMPORT);
        task.setParams(JSON.toJSONString(params));
        task.setStatus(STATUS_PENDING);
        task.setTotalRows(0);
        task.setCreateTime(LocalDateTime.now());
        exportTaskDao.insert(task);

        log.info("导入任务已创建: taskId={}, userId={}, file={}", taskId, userId, originalFilename);
        return taskId;
    }

    /**
     * Step 2：后台异步解析 Excel + 并行批量插入
     *
     * 流程：EasyExcel 逐行流式读取（低内存）→ 攒够 2000 行 → 提交异步 INSERT
     *      每攒够 10 个 Future → 等待全部完成（流控内存，最多 2 万行在内存中）
     *      全部读完 → 等待剩余 Future → 更新任务状态 DONE
     */
    @Async("exportTaskExecutor")
    public void processImportAsync(String taskId, String tempFilePath) {
        updateTaskStatus(taskId, STATUS_PROCESSING, null, null, null, null);

        AtomicInteger totalRows = new AtomicInteger(0);

        // buffer / pendingFutures 只在 EasyExcel 回调线程（单线程）中读写，无并发问题
        List<ShareDTO>               buffer         = new ArrayList<>(IMPORT_BATCH_SIZE);
        List<CompletableFuture<Void>> pendingFutures = new ArrayList<>();

        try {
            EasyExcel.read(tempFilePath, ShareImportDTO.class,
                    new AnalysisEventListener<ShareImportDTO>() {

                        @Override
                        public void invoke(ShareImportDTO row, AnalysisContext ctx) {
                            ShareDTO share = convertImportRow(row);
                            if (share == null) return;  // 跳过空标题等无效行
                            buffer.add(share);

                            if (buffer.size() >= IMPORT_BATCH_SIZE) {
                                flushBuffer();
                            }
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext ctx) {
                            if (!buffer.isEmpty()) flushBuffer();
                            // 等待所有剩余批次完成
                            waitAll();
                        }

                        /** 把 buffer 快照提交到线程池，DB 写入与 ES 索引并行执行，并清空 buffer */
                        private void flushBuffer() {
                            List<ShareDTO> batch = new ArrayList<>(buffer);
                            buffer.clear();
                            totalRows.addAndGet(batch.size());

                            // DB 写入和 ES 索引同时提交，互不等待
                            CompletableFuture<Void> dbFuture = CompletableFuture.runAsync(
                                    () -> shareDao.batchInsert(batch), queryExecutor);
                            CompletableFuture<Void> esFuture = CompletableFuture.runAsync(() -> {
                                try {
                                    shareEsService.bulkIndex(batch);
                                } catch (Exception e) {
                                    // ES 同步失败不阻断导入，记录日志后继续
                                    log.warn("导入批次 ES 索引失败，本批 {} 条将在下次全量同步补偿: {}",
                                            batch.size(), e.getMessage());
                                }
                            }, queryExecutor);

                            // 等待 DB 和 ES 都完成再继续（保证流控有效）
                            pendingFutures.add(CompletableFuture.allOf(dbFuture, esFuture));

                            // 流控：每攒 IMPORT_MAX_FUTURES 个 Future 等一次，防内存爆炸
                            if (pendingFutures.size() >= IMPORT_MAX_FUTURES) {
                                waitAll();
                            }
                        }

                        private void waitAll() {
                            if (!pendingFutures.isEmpty()) {
                                CompletableFuture.allOf(
                                        pendingFutures.toArray(new CompletableFuture[0])).join();
                                pendingFutures.clear();
                            }
                        }
                    }).sheet().doRead();

            updateTaskStatus(taskId, STATUS_DONE, totalRows.get(), null, null, LocalDateTime.now());
            log.info("导入完成: taskId={}, totalRows={}", taskId, totalRows.get());

        } catch (Exception e) {
            log.error("导入失败: taskId={}", taskId, e);
            String msg = e.getMessage();
            if (msg != null && msg.length() > 900) msg = msg.substring(0, 900);
            updateTaskStatus(taskId, STATUS_FAILED, totalRows.get(), null, msg, LocalDateTime.now());
        } finally {
            new File(tempFilePath).delete();  // 无论成败都删临时文件
        }
    }

    /** Step 3：查询导入任务状态（复用导出的方法，逻辑完全一样） */
    public List<ExportTaskVO> listMyImports(String userId, int limit) {
        return exportTaskDao.selectByUser(userId, BIZ_TYPE_IMPORT, limit)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 将 Excel 行转换为 ShareDTO，title 为空则返回 null（调用方跳过） */
    private ShareDTO convertImportRow(ShareImportDTO row) {
        if (!StringUtils.hasText(row.getTitle())) return null;

        ShareDTO share = new ShareDTO();
        share.setId(UUID.randomUUID().toString().replace("-", ""));
        share.setUserId(row.getUserId());
        share.setTitle(row.getTitle());
        share.setAuthor(row.getAuthor());
        share.setIsOriginal("是".equals(row.getIsOriginal()));
        share.setPrice(row.getPrice());
        share.setBuyCount(row.getBuyCount());
        share.setSummary(row.getSummary());
        share.setDownloadUrl(row.getDownloadUrl());
        share.setCover(row.getCover());
        share.setShowFlag("是".equals(row.getShowFlag()));
        share.setAuditStatus(StringUtils.hasText(row.getAuditStatus()) ? row.getAuditStatus() : "NOT_YET");
        share.setReason(row.getReason());
        share.setCreateTime(new Date());
        share.setUpdateTime(new Date());
        return share;
    }

    private ExportTaskVO toVO(ExportTaskDO task) {
        ExportTaskVO vo = new ExportTaskVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setMessage(task.getErrorMsg());
        vo.setParams(task.getParams());
        vo.setTotalRows(task.getTotalRows() == null ? 0 : task.getTotalRows());
        // 可下载 = 状态DONE + 文件路径存在
        vo.setDownloadable(STATUS_DONE.equals(task.getStatus())
                && task.getFilePath() != null);
        vo.setCreateTime(task.getCreateTime());
        vo.setFinishTime(task.getFinishTime());
        return vo;
    }

    // ============ Step 3b：下载文件 ============

    public void downloadExport(String taskId, String currentUserId,
                               HttpServletResponse response) throws IOException {
        ExportTaskDO task = exportTaskDao.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("导出任务不存在: " + taskId);
        }
        if (!task.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权下载他人导出文件");
        }
        if (!STATUS_DONE.equals(task.getStatus())) {
            throw new RuntimeException("文件尚未就绪，当前状态: " + task.getStatus());
        }
        if (task.getFilePath() == null) {
            throw new RuntimeException("文件已过期或被清理，请重新发起导出");
        }
        File file = new File(task.getFilePath());
        if (!file.exists()) {
            // DB 里记录了路径但磁盘文件丢了（发版清盘、人工删除），自修复 DB 状态
            exportTaskDao.markExpired(taskId);
            throw new RuntimeException("文件已丢失，请重新发起导出");
        }

        String encodedFileName = URLEncoder.encode("share-export-" + taskId + ".xlsx",
                StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedFileName);

        Files.copy(file.toPath(), response.getOutputStream());
        response.flushBuffer();
    }

    // ============ 定时清理（每小时跑一次）============

    /**
     * 清理策略：
     *  1. 找出 create_time 超过 2 小时 且 file_path 还不为空 的记录
     *  2. 删除磁盘文件
     *  3. UPDATE file_path=NULL, status='EXPIRED'（保留 DB 记录，方便用户看历史）
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanupExpiredTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        List<ExportTaskDO> expired = exportTaskDao.selectExpired(cutoff);
        for (ExportTaskDO task : expired) {
            try {
                if (task.getFilePath() != null) {
                    File f = new File(task.getFilePath());
                    if (f.exists()) f.delete();
                }
                exportTaskDao.markExpired(task.getId());
                log.info("清理过期导出任务: taskId={}", task.getId());
            } catch (Exception e) {
                log.warn("清理任务失败: taskId={}, err={}", task.getId(), e.getMessage());
            }
        }
    }

    // ============ 其他业务方法（不变）============

    public ShareDTO auditById(@PathVariable String id, ShareAuditDTO shareAuditDTO) {
        ShareDTO shareDTO = shareDao.selectByPrimaryKey(id);
        if (shareDTO == null || !shareDTO.getAuditStatus().equals("NOT_YET")) {
            throw new RuntimeException("参数非法!分享不存在或者状态不是待审核");
        }

        if (shareAuditDTO.getStatus().equals(AuditStatusEnum.PASS)) {
            String bizId = shareDTO.getId();

            UserAddBonusMsgDTO dto = new UserAddBonusMsgDTO();
            dto.setUserId(shareDTO.getUserId());
            dto.setBonus(50);
            dto.setBizId(bizId);

            log.info("开始发送 add-bonus 消息，userId: {}, bonus: {}", dto.getUserId(), dto.getBonus());
            UUID transactionId = UUID.randomUUID();
            this.source.output().send(
                    MessageBuilder.withPayload(dto)
                            .setHeader(RocketMQHeaders.TRANSACTION_ID, transactionId)
                            .setHeader("share_id", id)
                            .setHeader("dto", JSON.toJSONString(shareAuditDTO))
                            .build()
            );
            log.info("add-bonus 消息发送成功");
        } else {
            auditByIdInDB(id, shareAuditDTO);
        }

        return shareDTO;
    }

    public void auditByIdInDB(String id, ShareAuditDTO shareAuditDTO) {
        ShareDTO shareDTO = new ShareDTO();
        shareDTO.setId(id);
        shareDTO.setAuditStatus(shareAuditDTO.getStatus().toString());
        shareDTO.setReason(shareAuditDTO.getReason());
        shareDao.updateByPrimaryKeySelective(shareDTO);

        // 审核状态变更后同步到 ES（局部更新，只改两个字段，无需全量 re-index）
        try {
            shareEsService.updateAuditStatus(id, shareAuditDTO.getStatus().toString(),
                    shareAuditDTO.getReason());
        } catch (Exception e) {
            // ES 同步失败不影响主流程，记录日志等待下次全量同步修复
            log.warn("ES 审核状态同步失败，id={}, err={}", id, e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditByIdWithRocketMqLog(String id, ShareAuditDTO shareAuditDTO, String transactionId) {
        this.auditByIdInDB(id, shareAuditDTO);

        RocketMQTransactionLog rocketMQTransactionLog = new RocketMQTransactionLog();
        rocketMQTransactionLog.setTransactionId(transactionId);
        rocketMQTransactionLog.setLog("审核分享事务");
        rocketMQTransactionLogDao.insert(rocketMQTransactionLog);
    }

    /**
     * 分页查询分享列表。
     *
     * 优先走 ES 全文检索（毫秒级），ES 不可用时自动降级到 MySQL（保证可用性）。
     *
     * ES vs MySQL 性能对比：
     *   MySQL：title LIKE '%xxx%' 全表扫描，随数据量增长线性变慢，百万行约 1~5 秒
     *   ES：倒排索引查询，无论数据量多大基本维持在 10~50ms
     */
    public PageInfo<ShareDTO> queryShareList(Integer pageNo, Integer pageSize, String title) {
        try {
            // ES 分页从 0 开始，前端传来的 pageNo 从 1 开始，所以减 1
            ShareEsService.SearchResult result = shareEsService.searchByTitle(title, pageNo - 1, pageSize);

            List<ShareDTO> list = result.getDocs().stream()
                    .map(ShareEsService::convertToDTO)
                    .collect(Collectors.toList());

            // 手动构造 PageInfo，保持接口返回格式与 MySQL 版本完全兼容
            PageInfo<ShareDTO> pageInfo = new PageInfo<>(list);
            pageInfo.setTotal(result.getTotal());
            pageInfo.setPageNum(pageNo);
            pageInfo.setPageSize(pageSize);
            pageInfo.setSize(list.size());
            pageInfo.setPages((int) Math.ceil((double) result.getTotal() / pageSize));
            return pageInfo;

        } catch (Exception e) {
            // ES 不可用（未启动/网络异常）时降级到 MySQL，保证服务不中断
            log.warn("ES 查询异常，降级到 MySQL: {}", e.getMessage());
            PageHelper.startPage(pageNo, pageSize);
            List<ShareDTO> shareList = shareDao.selectShareList(title);
            return new PageInfo<>(shareList);
        }
    }

    /**
     * 触发 ES 全量同步（异步执行，不阻塞 HTTP 响应）。
     * 由管理员手动调用，适用于服务初始化或 ES 数据修复场景。
     */
    @Async("exportTaskExecutor")
    public void triggerEsFullSync() {
        shareEsService.fullSync();
    }

    private List<ShareExportDTO> convertToExportDTO(List<ShareDTO> batch) {
        List<ShareExportDTO> result = new ArrayList<>(batch.size());
        for (ShareDTO item : batch) {
            ShareExportDTO dto = new ShareExportDTO();
            dto.setId(item.getId());
            dto.setUserId(item.getUserId());
            dto.setTitle(item.getTitle());
            dto.setAuthor(item.getAuthor());
            dto.setIsOriginal(Boolean.TRUE.equals(item.getIsOriginal()) ? "是" : "否");
            dto.setPrice(item.getPrice());
            dto.setBuyCount(item.getBuyCount());
            dto.setAuditStatus(item.getAuditStatus());
            dto.setReason(item.getReason());
            dto.setShowFlag(Boolean.TRUE.equals(item.getShowFlag()) ? "是" : "否");
            dto.setCreateTime(item.getCreateTime());
            dto.setUpdateTime(item.getUpdateTime());
            dto.setSummary(item.getSummary());
            dto.setDownloadUrl(item.getDownloadUrl());
            dto.setCover(item.getCover());
            result.add(dto);
        }
        return result;
    }
}
