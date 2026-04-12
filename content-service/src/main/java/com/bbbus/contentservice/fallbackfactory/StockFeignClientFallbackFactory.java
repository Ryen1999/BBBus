package com.bbbus.contentservice.fallbackfactory;

import com.bbbus.contentservice.client.StockFeignClient;
import com.example.user.api.dto.StockDeductRequest;
import com.example.user.api.dto.StockResponse;
import com.example.user.api.dto.UserInfoResDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StockFeignClientFallbackFactory implements FallbackFactory<StockFeignClient> {

    @Override
    public StockFeignClient create(Throwable cause) {
        return new StockFeignClient() {
            @Override
            public StockResponse getStock(String s) {
                return null;
            }

            @Override
            public String deduct(StockDeductRequest stockDeductRequest) {
                return "";
            }

            @Override
            public List<UserInfoResDTO> list() {
                log.error("限流异常111111111111111:", cause);
                return Collections.emptyList();
            }
        };
    }
}
