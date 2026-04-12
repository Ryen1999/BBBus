package com.bbbus.contentservice.domain.entity.content;

import lombok.Data;

@Data
public class Share {

    private String id;

    private String userId;

    private String auditStatus;

    private String reason;

}
