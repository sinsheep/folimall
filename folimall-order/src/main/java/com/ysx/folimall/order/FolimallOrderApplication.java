package com.ysx.folimall.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class FolimallOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(FolimallOrderApplication.class, args);
	}

}
