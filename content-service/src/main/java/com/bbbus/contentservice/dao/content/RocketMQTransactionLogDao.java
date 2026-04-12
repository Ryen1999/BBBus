package com.bbbus.contentservice.dao.content;

import com.bbbus.contentservice.dto.messaging.RocketMQTransactionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RocketMQTransactionLogDao {

    void insert(RocketMQTransactionLog record);

    RocketMQTransactionLog selectByTransactionId(String transactionId);
}
