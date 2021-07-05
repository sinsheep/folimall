package com.ysx.folimall.ware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class FolimallWareApplication {

	public static void main(String[] args) {
		SpringApplication.run(FolimallWareApplication.class, args);
	}

}
