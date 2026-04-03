package com.bbbus.contentservice.rocketmq;

import com.alibaba.fastjson.JSON;
import com.bbbus.contentservice.dao.content.RocketMQTransactionLogDao;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.dto.messaging.RocketMQTransactionLog;
import com.bbbus.contentservice.service.ShareService;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// ① 注解换成 @Component，Bean 名字要和 yaml 里 transactionListener 的值一致
@Component("addBonusStreamListener")
public class AddBonusStreamListener  implements TransactionListener {
    @Autowired
    private ShareService shareService;

    @Autowired
    private RocketMQTransactionLogDao rocketMQTransactionLogDao;

    // ② 方法签名变了：入参是原生 RocketMQ 的 Message，不是 Spring 的 Message
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // ③ 从原生 Message 取属性用 msg.getProperty()，不是 getHeaders()
        String shareId       = msg.getProperty("share_id");
        String transactionId = msg.getProperty(RocketMQHeaders.TRANSACTION_ID);
        String dtoJson       = msg.getProperty("dto");
        ShareAuditDTO shareAuditDTO = JSON.parseObject(dtoJson, ShareAuditDTO.class);

        try {
            shareService.auditByIdWithRocketMqLog(shareId, shareAuditDTO, transactionId);
            // ④ 返回值换成原生枚举
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    // ⑤ 回查方法入参换成 MessageExt（MessageExt 是 Message 的子类，多了消息ID等信息）
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        String transactionId = msg.getProperty(RocketMQHeaders.TRANSACTION_ID);
        RocketMQTransactionLog log = rocketMQTransactionLogDao.selectByTransactionId(transactionId);

        if (log != null) {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
        return LocalTransactionState.ROLLBACK_MESSAGE;
    }
}
