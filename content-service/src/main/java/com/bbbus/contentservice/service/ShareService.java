package com.bbbus.contentservice.service;

import com.bbbus.contentservice.dao.content.ShareDao;
import com.bbbus.contentservice.domain.entity.content.Share;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.dto.messaging.UserAddBonusMsgDTO;
import com.bbbus.contentservice.enums.AuditStatusEnum;
import com.bbbus.contentservice.rocketmq.AddBonusSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Service
public class ShareService {

    @Autowired
    ShareDao shareDao;

    @Autowired
    RocketMQTemplate rocketMQTemplate;

    @Autowired
    private AddBonusSource addBonusSource;

    public Share auditById(@PathVariable String id, ShareAuditDTO shareAuditDTO)
    {
        //1.查询当前Shares是否存在,不存在或者当前的状态不是待审核状态，则抛异常
        Share share =shareDao.selectByPrimaryKey( id);
        if(share == null || !share.getAuditStatus().equals("NOT_YET"))
        {

            throw new RuntimeException("参数非法!分享不存在或者状态不是待审核");
        }

        //2.更新Shares的状态为审核通过或者审核未通过
        share.setAuditStatus(shareAuditDTO.getStatus().toString());
        shareDao.updateByPrimaryKeySelective(share);
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
            rocketMQTemplate.convertAndSend("add-bonus", dto);
            //rocketMQTemplate.sendMessageInTransaction("add-bonus-group","add-bonus", MessageBuilder.withPayload(dto).build(), shareAuditDTO);
            log.info("add-bonus 消息发送成功");
        }

        return share;
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
