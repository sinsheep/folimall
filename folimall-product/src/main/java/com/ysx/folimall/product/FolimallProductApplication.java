package com.ysx.folimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


//@MapperScan("com.ysx.folimall.product.dao")
@EnableDiscoveryClient
@SpringBootApplication
public class FolimallProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(FolimallProductApplication.class, args);
	}

}
