package com.bbbus.contentservice.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    @SentinelResource("common")
    public String common()
    {
        System.out.println("common");
        return "test_common";
    }
}
