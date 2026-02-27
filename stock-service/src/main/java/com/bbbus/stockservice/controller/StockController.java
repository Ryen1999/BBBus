package com.bbbus.stockservice.controller;

import com.bbbus.stock.api.StockApi;
import com.bbbus.stock.api.dto.StockDeductRequest;
import com.bbbus.stock.api.dto.StockResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class StockController implements StockApi {

    private final Map<String, Integer> stockMap = new ConcurrentHashMap<>();

    public StockController() {
        stockMap.put("SKU-1", 100);
        stockMap.put("SKU-2", 50);
    }

    @Override
    public StockResponse getStock(String skuCode) {
        Integer available = stockMap.getOrDefault(skuCode, 0);
        return new StockResponse(skuCode, available);
    }

    @Override
    public String deduct(StockDeductRequest request) {
        String skuCode = request.getSkuCode();
        int count = request.getCount() == null ? 0 : request.getCount();
        stockMap.compute(skuCode, (k, v) -> {
            int current = v == null ? 0 : v;
            return Math.max(current - count, 0);
        });
        return "deducted";
    }
}
