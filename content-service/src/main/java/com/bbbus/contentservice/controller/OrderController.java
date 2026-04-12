package com.bbbus.contentservice.controller;

import com.bbbus.contentservice.client.StockFeignClient;
import com.example.user.api.dto.StockDeductRequest;
import com.example.user.api.dto.StockResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *  订单管理
 *
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private  StockFeignClient stockFeignClient;
    
    public OrderController(StockFeignClient stockFeignClient) {
        this.stockFeignClient = stockFeignClient;
    }
    
    /**
     * 创建订单
     * @Author 郭旺
     * @param skuCode 商品编码
     * @param count   购买数量
     * @return 订单结果
     *
     */
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
