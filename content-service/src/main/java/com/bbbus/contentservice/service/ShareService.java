package com.bbbus.contentservice.service;

import com.bbbus.contentservice.dao.content.ShareDao;
import com.bbbus.contentservice.domain.entity.content.Share;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.dto.messaging.UserAddBonusMsgDTO;
import com.bbbus.contentservice.enums.AuditStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Service
public class ShareService {

    @Autowired
    ShareDao shareDao;

    @Autowired
    RocketMQTemplate rocketMQTemplate;


    public Share auditById(@PathVariable Integer id, ShareAuditDTO shareAuditDTO)
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
        UserAddBonusMsgDTO dto = new UserAddBonusMsgDTO();
        dto.setUserId(share.getUserId());
        dto.setBonus(50);
        rocketMQTemplate.convertAndSend("add-bonus", dto);
        return share;
    }

}
