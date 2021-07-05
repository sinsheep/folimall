package com.ysx.folimall.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class FolimallCouponApplication {

	public static void main(String[] args) {
		SpringApplication.run(FolimallCouponApplication.class, args);
	}

}
