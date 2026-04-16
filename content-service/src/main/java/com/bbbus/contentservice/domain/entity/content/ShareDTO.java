package com.bbbus.contentservice.domain.entity.content;

import lombok.Data;
import java.util.Date;

@Data
public class ShareDTO {

    private String id;

    private String userId;

    private String title;

    private Date createTime;

    private Date updateTime;

    private Boolean isOriginal;

    private String author;

    private String cover;

    private String summary;

    private Integer price;

    private String downloadUrl;

    private String buyCount;

    private Boolean showFlag;

    private String auditStatus;

    private String reason;

}
