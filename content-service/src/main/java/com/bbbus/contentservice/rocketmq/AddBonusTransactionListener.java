package com.bbbus.contentservice.rocketmq;

import com.alibaba.fastjson.JSON;
import com.bbbus.contentservice.dao.content.RocketMQTransactionLogDao;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.dto.messaging.RocketMQTransactionLog;
import com.bbbus.contentservice.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;


// 监听事务消息
@Slf4j
@RocketMQTransactionListener(txProducerGroup = "tx-add-bonus-group")// 事务组的group一定是和消息发送的group一致
public class AddBonusTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private ShareService shareService;

    @Autowired
    private RocketMQTransactionLogDao rocketMQTransactionLogDao;

    // 执行本地事务
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {
        String transactionId = null;
        try {
            // 安全获取消息头信息
            Object shareIdObj = message.getHeaders().get("share_id");
            Object transactionIdObj = message.getHeaders().get(RocketMQHeaders.TRANSACTION_ID);
            Object dtoObj = message.getHeaders().get("dto");

            if (shareIdObj == null || transactionIdObj == null || dtoObj == null) {
                log.error("消息头信息不完整，shareId: {}, transactionId: {}, dto: {}", 
                    shareIdObj, transactionIdObj, dtoObj);
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            String shareId = shareIdObj.toString();
            transactionId = transactionIdObj.toString();
            String dto = dtoObj.toString();
            
            ShareAuditDTO shareAuditDTO = JSON.parseObject(dto, ShareAuditDTO.class);
            
            // 更新share为PASS并插入日志
            shareService.auditByIdWithRocketMqLog(shareId, shareAuditDTO, transactionId);
            
            log.info("本地事务执行成功，transactionId: {}, shareId: {}", transactionId, shareId);
            // 提交事务
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            log.error("本地事务执行失败，transactionId: {}, 错误信息: {}", transactionId, e.getMessage(), e);
            //回滚事务
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }


    // 检查本地事务
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        try {
            Object transactionIdObj = message.getHeaders().get(RocketMQHeaders.TRANSACTION_ID);
            if (transactionIdObj == null) {
                log.error("检查本地事务时，transactionId为空");
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            
            String transactionId = transactionIdObj.toString();
            RocketMQTransactionLog rocketMQTransactionLog = rocketMQTransactionLogDao.selectByTransactionId(transactionId);

            if (rocketMQTransactionLog != null) {
                log.info("检查本地事务成功，transactionId: {}", transactionId);
                return RocketMQLocalTransactionState.COMMIT;
            }
            
            log.warn("检查本地事务失败，未找到transactionId: {} 对应的日志记录", transactionId);
            return RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e) {
            log.error("检查本地事务时发生异常: {}", e.getMessage(), e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }
}
