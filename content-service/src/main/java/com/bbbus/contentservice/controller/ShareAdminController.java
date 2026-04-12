package com.bbbus.contentservice.controller;

import com.bbbus.contentservice.aop.annotation.CheckAuthorization;
import com.bbbus.contentservice.domain.entity.content.Share;
import com.bbbus.contentservice.dto.content.ShareAuditDTO;
import com.bbbus.contentservice.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  分享管理
 *
 */

@Slf4j
@RestController
@RequestMapping("/admin/shares")
public class ShareAdminController {

    @Autowired
    ShareService shareService;


    /**
     * 分享书籍
     * @Author 郭旺
     * @param id
     * @param shareAuditDTO
     * @return
     */
    @PutMapping("/audit/{id}")
    @CheckAuthorization("ADMIN")
    public Share auditById(@PathVariable String id, ShareAuditDTO shareAuditDTO)
    {
        // TODO 认证、授权
        return this.shareService.auditById(id,shareAuditDTO);
    }
}
