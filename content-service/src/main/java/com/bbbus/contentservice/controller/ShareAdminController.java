package com.bbbus.contentservice.controller;

import com.bbbus.contentservice.aop.annotation.CheckAuthorization;
import com.bbbus.contentservice.common.response.ApiResponse;
import com.bbbus.contentservice.domain.entity.content.ShareDTO;
import com.bbbus.contentservice.dto.ExportTaskVO;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.security.UserContext;
import com.bbbus.contentservice.service.ShareService;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shares")
public class ShareAdminController {

    @Autowired
    private ShareService shareService;

    @Autowired
    private UserContext userContext;   // 从当前请求取 userId

    @PutMapping("/audit/{id}")
    @CheckAuthorization("ADMIN")
    public ShareDTO auditById(@PathVariable String id, ShareAuditDTO shareAuditDTO) {
        return this.shareService.auditById(id, shareAuditDTO);
    }

    @GetMapping("/q")
    @CheckAuthorization("user")
    public PageInfo<ShareDTO> queryShareList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title) {
        log.info("查询分享列表: pageNo={}, pageSize={}, title={}", pageNo, pageSize, title);
        return shareService.queryShareList(pageNo, pageSize, title);
    }

    // ============ 导出相关 4 个端点（DB 持久化版本）============

    /**
     * 【步骤 1】触发导出
     * 在 DB 里 INSERT 一条 PENDING 任务，立即返回 taskId
     */
    @PostMapping("/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @CheckAuthorization("user")
    public ApiResponse<ExportTaskVO> triggerExport(@RequestParam(required = false) String title) {
        String userId = userContext.getCurrentUserId();
        log.info("触发导出任务: userId={}, title={}", userId, title);

        String taskId = shareService.createExportTask(userId, title);   // 同步：DB INSERT
        shareService.processExportAsync(taskId, title);                  // 异步：立即返回
        return ApiResponse.success(shareService.getExportTaskStatus(taskId, userId));
    }

    /**
     * 【步骤 2】查询任务状态
     * 带越权校验：只能看自己的任务
     */
    @GetMapping("/export/{taskId}")
    @CheckAuthorization("user")
    public ApiResponse<ExportTaskVO> getExportStatus(@PathVariable String taskId) {
        String userId = userContext.getCurrentUserId();
        return ApiResponse.success(shareService.getExportTaskStatus(taskId, userId));
    }

    /**
     * 【步骤 3】下载文件
     */
    @GetMapping("/export/{taskId}/download")
    @CheckAuthorization("user")
    public void downloadExport(@PathVariable String taskId,
                               HttpServletResponse response) throws IOException {
        String userId = userContext.getCurrentUserId();
        log.info("下载导出文件: taskId={}, userId={}", taskId, userId);
        shareService.downloadExport(taskId, userId, response);
    }

    /**
     * 【新增】我的导出记录
     */
    @GetMapping("/export/my")
    @CheckAuthorization("user")
    public ApiResponse<List<ExportTaskVO>> myExports(
            @RequestParam(defaultValue = "10") int limit) {
        String userId = userContext.getCurrentUserId();
        return ApiResponse.success(shareService.listMyExports(userId, limit));
    }

    // ============ 导入相关 3 个端点（与导出完全对称）============

    /**
     * 【导入步骤 1】上传文件，触发异步导入，立即返回 taskId
     * 前端用 multipart/form-data 提交，参数名 file
     */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<ExportTaskVO> triggerImport(@RequestParam("file") MultipartFile file)
            throws IOException {
        String userId = userContext.getCurrentUserId();
        log.info("触发导入任务: userId={}, filename={}, size={}MB",
                userId, file.getOriginalFilename(), file.getSize() / 1024 / 1024);

        // 把上传文件落盘到临时目录（@Async 需要文件路径，不能用 InputStream）
        File tempFile = File.createTempFile("share-import-", ".xlsx");
        file.transferTo(tempFile);

        String taskId = shareService.createImportTask(userId, file.getOriginalFilename());
        shareService.processImportAsync(taskId, tempFile.getAbsolutePath());  // 异步，立即返回
        return ApiResponse.success(shareService.getExportTaskStatus(taskId, userId));
    }

    /**
     * 【导入步骤 2】轮询导入任务状态（复用导出的查询方法）
     */
    @GetMapping("/import/{taskId}")
    public ApiResponse<ExportTaskVO> getImportStatus(@PathVariable String taskId) {
        String userId = userContext.getCurrentUserId();
        return ApiResponse.success(shareService.getExportTaskStatus(taskId, userId));
    }

    /**
     * 【新增】我的导入记录
     */
    @GetMapping("/import/my")
    public ApiResponse<List<ExportTaskVO>> myImports(
            @RequestParam(defaultValue = "10") int limit) {
        String userId = userContext.getCurrentUserId();
        return ApiResponse.success(shareService.listMyImports(userId, limit));
    }

    // ============ ES 运维接口 ============

    /**
     * 触发 MySQL → ES 全量同步（异步执行，立即返回）。
     *
     * 适用场景：
     *   1. 服务首次上线，ES 索引为空
     *   2. ES 宕机恢复后，期间 MySQL 有写操作导致数据不一致
     *
     * 注意：同步过程中查询结果可能短暂不完整（索引先清空后重建）。
     */
    @PostMapping("/es/sync")
    //@CheckAuthorization("ADMIN")
    public ApiResponse<String> syncToEs() {
        log.info("管理员触发 ES 全量同步");
        shareService.triggerEsFullSync();
        return ApiResponse.success("ES 全量同步已在后台启动，请查看服务日志跟踪进度");
    }
}
