package com.bbbus.contentservice.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.bbbus.contentservice.client.StockFeignClient;
import com.bbbus.contentservice.service.TestService;
import com.example.user.api.dto.UserInfoResDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shares")
public class TestController {
	
	@Autowired
	private DiscoveryClient discoveryClient;
	
	@Autowired
	private StockFeignClient stockFeignClient;
	
	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private TestService testService;
	
	@GetMapping("/test")
	public List query() {
		List list = new ArrayList();
		list.add(discoveryClient.getInstances("user-service"));
		System.out.println("query");
		return list;
	}
	@GetMapping("/list")
	public List<UserInfoResDTO> list() {
		List<UserInfoResDTO> list = stockFeignClient.list();

		return list;
	}

	@GetMapping("/correlationTest")
	public String correlationTest() throws InterruptedException {
		String result = "";
		for (int i = 0; i < 100; i++)
		{
			 result = restTemplate.getForObject("http://user-service/api/user/list", String.class);
			Thread.sleep(100);
		}
		return result;
	}

	@GetMapping("/test-a")
	public String testA() throws InterruptedException {
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
			entry = SphU.entry("test-sentinel-api");
        } catch (BlockException e) {


			return "限流，被降级了";
        }finally {
			if (entry != null) {
				entry.exit();
			}
		}


        return a;
	}
}
