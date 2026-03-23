package com.bbbus.stock.api;

import com.bbbus.stock.api.dto.StockDeductRequest;
import com.bbbus.stock.api.dto.StockResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface StockApi {
    
    @GetMapping("/api/stocks/{skuCode}")
    StockResponse getStock(@PathVariable("skuCode") String skuCode);
    
    @PostMapping("/api/stocks/deduct")
    String deduct(@RequestBody StockDeductRequest request);
}