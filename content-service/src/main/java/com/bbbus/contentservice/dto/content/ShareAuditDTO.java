package com.bbbus.contentservice.dto.content;

import com.bbbus.contentservice.enums.AuditStatusEnum;
import lombok.Data;

@Data
public class ShareAuditDTO {
    // 审核状态
    private AuditStatusEnum status;

    // 审核原因
    private String reason;
}
