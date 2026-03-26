package com.bbbus.contentservice.client;

import com.bbbus.contentservice.fallback.StockFeignClientFallback;
import com.bbbus.contentservice.fallbackfactory.StockFeignClientFallbackFactory;
import com.example.user.api.StockApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

@FeignClient(name = "user-service",
        //fallback = StockFeignClientFallback.class,
        fallbackFactory = StockFeignClientFallbackFactory.class)
@Component
public interface StockFeignClient extends StockApi {
}
