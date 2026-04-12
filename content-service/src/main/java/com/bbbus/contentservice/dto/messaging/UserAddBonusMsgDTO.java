package com.bbbus.contentservice.dto.messaging;

import com.bbbus.contentservice.dto.BaseObject;
import lombok.Builder;
import lombok.Data;

@Data
//@Builder
public class UserAddBonusMsgDTO extends BaseObject {

    private String bizId; // 唯一业务ID，比如 shareId

    /**
     * 为谁增加积分
     */
    private String userId;

    /**
     * 加多少积分
     */
    private Integer bonus;

}
