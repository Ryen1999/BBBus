package com.bbbus.contentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@EnableFeignClients
@SpringBootApplication
@EnableAsync
@EnableBinding(Source.class)
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }
    
    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
