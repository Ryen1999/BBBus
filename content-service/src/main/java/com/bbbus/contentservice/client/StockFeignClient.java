package com.bbbus.contentservice.client;

import com.example.user.api.StockApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

@FeignClient(name = "user-service")
@Component
public interface StockFeignClient extends StockApi {
}
