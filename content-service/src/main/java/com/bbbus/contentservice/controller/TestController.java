package com.bbbus.contentservice.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.bbbus.contentservice.client.StockFeignClient;
import com.bbbus.contentservice.service.TestService;
import com.example.user.api.dto.UserInfoResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/shares")
public class TestController {

    private static final String USER_SERVICE = "user-service";
    private static final String USER_LIST_URL = "http://user-service/api/user/list";
    private static final String SENTINEL_RESOURCE = "test-sentinel-api";

    private final DiscoveryClient discoveryClient;
    private final StockFeignClient stockFeignClient;
    private final RestTemplate restTemplate;
    private final TestService testService;

    @GetMapping("/test")
    public List<ServiceInstance> query() {
        List<ServiceInstance> instances = discoveryClient.getInstances(USER_SERVICE);
        log.info("discovered {} instances for {}", instances.size(), USER_SERVICE);
        return instances;
    }

    @GetMapping("/list")
    public List<UserInfoResDTO> list() {
        List<UserInfoResDTO> users = stockFeignClient.list();
        return users == null ? Collections.emptyList() : users;
    }

    @GetMapping("/correlationTest")
    public String correlationTest(@RequestParam(defaultValue = "100") Integer loopCount,
                                  @RequestParam(defaultValue = "100") Long sleepMs) {
        int safeLoopCount = Math.max(1, Math.min(loopCount, 500));
        long safeSleepMs = Math.max(0L, Math.min(sleepMs, 1000L));
        String result = "";

        for (int i = 0; i < safeLoopCount; i++) {
            result = restTemplate.getForObject(USER_LIST_URL, String.class);
            if (safeSleepMs <= 0) {
                continue;
            }
            try {
                Thread.sleep(safeSleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("correlationTest interrupted at round {}", i + 1, e);
                break;
            }
        }
        return result;
    }

    @GetMapping("/test-a")
    public String testA() {
        testService.common();
        return "test-a";
    }

    @GetMapping("/test-b")
    public String testB() {
        testService.common();
        return "test-b";
    }

    @GetMapping("/test-sentinel")
    public String testSentinelApi(@RequestParam(required = false) String a) {
        Entry entry = null;
        try {
            entry = SphU.entry(SENTINEL_RESOURCE);
            return a == null ? "" : a;
        } catch (BlockException e) {
            log.warn("sentinel blocked for resource: {}", SENTINEL_RESOURCE, e);
            return "限流，已降级";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
