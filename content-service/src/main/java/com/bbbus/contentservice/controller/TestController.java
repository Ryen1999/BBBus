package com.bbbus.contentservice.controller;

import com.bbbus.contentservice.client.StockFeignClient;
import com.example.user.api.dto.UserInfoResDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/shares")
public class TestController {
	
	@Autowired
	private DiscoveryClient discoveryClient;
	
	@Autowired
	private StockFeignClient stockFeignClient;
	
	@Autowired
	private RestTemplate restTemplate;
	
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
}
