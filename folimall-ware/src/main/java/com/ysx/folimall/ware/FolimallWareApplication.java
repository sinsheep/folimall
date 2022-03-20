package com.ysx.folimall.ware;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ysx.folimall.ware.feign")
@SpringBootApplication
@EnableRabbit
public class FolimallWareApplication {

	public static void main(String[] args) {
		SpringApplication.run(FolimallWareApplication.class, args);
	}

}
