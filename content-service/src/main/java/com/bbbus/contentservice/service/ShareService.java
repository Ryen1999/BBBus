package com.bbbus.contentservice.service;

import com.alibaba.fastjson.JSON;
import com.bbbus.contentservice.dao.content.RocketMQTransactionLogDao;
import com.bbbus.contentservice.dao.content.ShareDao;
import com.bbbus.contentservice.domain.entity.content.Share;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.dto.messaging.RocketMQTransactionLog;
import com.bbbus.contentservice.dto.messaging.UserAddBonusMsgDTO;
import com.bbbus.contentservice.enums.AuditStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Slf4j
@Service
public class ShareService {

    @Autowired
    ShareDao shareDao;
    @Autowired
    private RocketMQTransactionLogDao rocketMQTransactionLogDao;

//    @Autowired
//    RocketMQTemplate rocketMQTemplate;

//    @Autowired
//    private AddBonusSource addBonusSource;
    @Autowired
    private Source source;

    public Share auditById(@PathVariable String id, ShareAuditDTO shareAuditDTO)
    {
        //1.查询当前Shares是否存在,不存在或者当前的状态不是待审核状态，则抛异常
        Share share =shareDao.selectByPrimaryKey( id);
        if(share == null || !share.getAuditStatus().equals("NOT_YET"))
        {

            throw new RuntimeException("参数非法!分享不存在或者状态不是待审核");
        }


        //3.如果PASS，则为发布人添加积分
        if(shareAuditDTO.getStatus().equals(AuditStatusEnum.PASS))
        {
            String bizId = share.getId();


            UserAddBonusMsgDTO dto = new UserAddBonusMsgDTO();
            dto.setUserId(share.getUserId());
            dto.setBonus(50);
            dto.setBizId(bizId);
            //发送消息
            log.info("开始发送 add-bonus 消息，userId: {}, bonus: {}", dto.getUserId(), dto.getBonus());
            UUID transactionId = UUID.randomUUID();
            //消息发送的group一定是和事务的group一致
            this.source.output()
                    .send(
                    MessageBuilder.withPayload(dto)
                            .setHeader(RocketMQHeaders.TRANSACTION_ID, transactionId)
                            .setHeader("share_id", id)
                            .setHeader("dto", JSON.toJSONString(shareAuditDTO))
                            .build()
            );
            log.info("add-bonus 消息发送成功");
        }else
        {
            auditByIdInDB(id, shareAuditDTO);
        }

        return share;
    }

    //更新Shares的状态为审核通过或者审核未通过
    public void auditByIdInDB(String id,ShareAuditDTO shareAuditDTO) {
        Share share = new Share();
        share.setId(id);
        share.setAuditStatus(shareAuditDTO.getStatus().toString());
        share.setReason(shareAuditDTO.getReason());
        shareDao.updateByPrimaryKeySelective(share);
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditByIdWithRocketMqLog(String id, ShareAuditDTO shareAuditDTO, String transactionId) {
        //1.更新Shares的状态为审核通过
        this.auditByIdInDB(id, shareAuditDTO);

        //2.插入事务日志
        RocketMQTransactionLog rocketMQTransactionLog =new RocketMQTransactionLog();
        rocketMQTransactionLog.setTransactionId(transactionId);
        rocketMQTransactionLog.setLog("审核分享事务");
        rocketMQTransactionLogDao.insert(rocketMQTransactionLog);
    }

//    public Share auditById(@PathVariable String id, ShareAuditDTO shareAuditDTO) {
//
//        Share share = shareDao.selectByPrimaryKey(id);
//
//        if (share == null || !share.getAuditStatus().equals("NOT_YET")) {
//            throw new RuntimeException("参数非法!分享不存在或者状态不是待审核");
//        }
//
//        share.setAuditStatus(shareAuditDTO.getStatus().toString());
//        shareDao.updateByPrimaryKeySelective(share);
//
//        if (shareAuditDTO.getStatus().equals(AuditStatusEnum.PASS)) {
//
//            String bizId = share.getId(); // 建议加前缀
//
//            UserAddBonusMsgDTO dto = new UserAddBonusMsgDTO();
//            dto.setUserId(share.getUserId());
//            dto.setBonus(50);
//            dto.setBizId(bizId);
//
//            log.info("开始发送 add-bonus 消息，userId: {}, bonus: {}", dto.getUserId(), dto.getBonus());
//
//            addBonusSource.output().send(
//                    MessageBuilder.withPayload(dto).build()
//            );
//
//            log.info("add-bonus 消息发送成功");
//        }
//
//        return share;
//    }
}
