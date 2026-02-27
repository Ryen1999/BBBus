package com.bbbus.orderservice.controller;

import com.bbbus.orderservice.client.StockFeignClient;
import com.bbbus.stock.api.dto.StockDeductRequest;
import com.bbbus.stock.api.dto.StockResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final StockFeignClient stockFeignClient;

    public OrderController(StockFeignClient stockFeignClient) {
        this.stockFeignClient = stockFeignClient;
    }

    @PostMapping("/place")
    public String placeOrder(@RequestParam String skuCode, @RequestParam Integer count) {
        StockResponse stock = stockFeignClient.getStock(skuCode);
        if (stock.getAvailable() < count) {
            return "库存不足，无法下单";
        }

        StockDeductRequest request = new StockDeductRequest();
        request.setSkuCode(skuCode);
        request.setCount(count);
        stockFeignClient.deduct(request);

        return "下单成功，已扣减库存";
    }
}
