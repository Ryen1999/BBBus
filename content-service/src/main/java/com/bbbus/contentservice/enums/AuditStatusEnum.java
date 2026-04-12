package com.bbbus.contentservice.enums;

import lombok.Data;


/**
 * 审核状态枚举类
 * 用于表示内容的审核状态
 */
public enum AuditStatusEnum {

    /**
     * 待审核状态
     * 表示内容已提交但尚未进行审计
     */
    NOT_YET,

    /**
     * 审核通过状态
     * 表示内容已通过审计审核
     */
    PASS,

    /**
     * 审核拒绝状态
     * 表示内容未通过审计审核被驳回
     */
    REJECT;
}