package com.ysx.folimall.order.config;

import com.rabbitmq.client.Channel;
import com.ysx.folimall.order.entity.OrderEntity;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Configuration
public class MyMQConfig {


    @Bean
    public Queue orderDelayQueue(){
        Map<String,Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);
        Queue queue = new Queue("order.delay.queue", true, false, false,arguments);
        return queue;
    }
    @Bean
    public Queue orderReleaseOrderQueue(){
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }
    @Bean
    public Exchange orderEventExchange(){
        return new TopicExchange("order-event-exchange",true,false);
    }
    @Bean
    public Binding orderCreateOrderBinding(){
        return new Binding("order.delay.queue",Binding.DestinationType.QUEUE
        ,"order-event-exchange",
                "order.create.order",null);
    }
    @Bean
    public Binding orderReleaseOrderBinding(){

        return new Binding("order.release.order.queue",Binding.DestinationType.QUEUE
                ,"order-event-exchange",
                "order.release.order",null);
    }


    public Binding orderReleaseOtherBinding(){

        return new Binding("stock.release.stock.queue",Binding.DestinationType.QUEUE
                ,"order-event-exchange",
                "order.release.other.#"
                ,null);
    }
}

