package com.bbbus.orderservice.client;

import com.bbbus.stock.api.StockApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "stock-service", url = "${stock.service.url:http://localhost:8081}")
public interface StockFeignClient extends StockApi {
}
