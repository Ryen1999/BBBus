package com.bbbus.contentservice.dto.messaging;

import com.bbbus.contentservice.dto.BaseObject;
import lombok.Data;

@Data
public class RocketMQTransactionLog extends BaseObject {

    //事务id
    private String transactionId;

    private String log;

}
