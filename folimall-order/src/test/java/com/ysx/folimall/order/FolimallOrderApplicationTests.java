package com.ysx.folimall.order;

import com.ysx.folimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@Slf4j
@SpringBootTest
class FolimallOrderApplicationTests {

	@Test
	void contextLoads() {
	}

	/**
	 * 创建exchange queue binding
	 */

	@Autowired
	AmqpAdmin amqpAdmin;
	@Test
	public void createExchange(){
		Exchange exchange = new DirectExchange("hello-java-exchange",true,false);
		amqpAdmin.declareExchange(exchange);
		log.info("exchange[{}] 创建成功","hello-java.exchange");
	}

	@Test
	public void createQueue(){
		Queue queue = new Queue("hello-java-queue",true,false,true);
		amqpAdmin.declareQueue(queue);
		log.info("queue[{}] 创建成功","hello-java.queue");
	}
	@Test
	public void createBinding(){
		Binding binding = new Binding("hello-java-queue"
		, Binding.DestinationType.QUEUE,"hello-java-exchange"
		,"hello.java",null);
		amqpAdmin.declareBinding(binding);
		log.info("binding[{}] 创建成功","hello-java-binding");
	}
	@Autowired
	RabbitTemplate rabbitTemplate;
	@Test
	public void sendMessageTest(){
		OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
		reasonEntity.setId(1L);
		reasonEntity.setCreateTime(new Date());
		reasonEntity.setName("haha");
		rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",reasonEntity);
		log.info("消息发送完成{}");
	}

}
