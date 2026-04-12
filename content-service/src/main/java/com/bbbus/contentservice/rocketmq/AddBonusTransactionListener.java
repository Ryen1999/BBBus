package com.bbbus.contentservice.rocketmq;

import com.alibaba.fastjson.JSON;
import com.bbbus.contentservice.dao.content.RocketMQTransactionLogDao;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.dto.messaging.RocketMQTransactionLog;
import com.bbbus.contentservice.service.ShareService;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;

// 监听事务消息
@RocketMQTransactionListener(txProducerGroup = "tx-add-bonus-group")// 事务组的group一定是和消息发送的group一致
public class AddBonusTransactionListener implements RocketMQLocalTransactionListener {


    @Autowired
    ShareService shareService;

    @Autowired
    RocketMQTransactionLogDao rocketMQTransactionLogDao;

    // 执行本地事务
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {

        String shareId =message.getHeaders().get("share_id").toString();
        String transactionId = message.getHeaders().get(RocketMQHeaders.TRANSACTION_ID).toString();
        String dto = message.getHeaders().get("dto").toString();
        ShareAuditDTO shareAuditDTO =JSON.parseObject(dto, ShareAuditDTO.class);
        try{
            // 更新share为PASS并插入日志
            shareService.auditByIdWithRocketMqLog(shareId,shareAuditDTO,transactionId);

            // 提交事务
            return RocketMQLocalTransactionState.COMMIT;
        }catch (Exception e)
        {
            //回滚事务
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }


    // 检查本地事务
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        String transactionId = message.getHeaders().get(RocketMQHeaders.TRANSACTION_ID).toString();
        RocketMQTransactionLog rocketMQTransactionLog =rocketMQTransactionLogDao.selectByTransactionId(transactionId);

        if(rocketMQTransactionLog != null)
        {
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
