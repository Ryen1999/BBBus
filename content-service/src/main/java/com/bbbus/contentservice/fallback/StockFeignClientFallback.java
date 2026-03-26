package com.bbbus.contentservice.fallback;

import com.bbbus.contentservice.client.StockFeignClient;
import com.example.user.api.dto.StockDeductRequest;
import com.example.user.api.dto.StockResponse;
import com.example.user.api.dto.UserInfoResDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class StockFeignClientFallback implements StockFeignClient {
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
        List list = new ArrayList();

        UserInfoResDTO userInfo = new UserInfoResDTO();
        userInfo.setWx_nickname("fallback");
        list.add(userInfo);
        return list;
    }
}
