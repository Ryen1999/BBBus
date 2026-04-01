package com.bbbus.contentservice.dto.messaging;

import lombok.Builder;
import lombok.Data;

@Data
//@Builder
public class UserAddBonusMsgDTO {

    /**
     * 为谁增加积分
     */
    private Integer userId;

    /**
     * 加多少积分
     */
    private Integer bonus;

}
