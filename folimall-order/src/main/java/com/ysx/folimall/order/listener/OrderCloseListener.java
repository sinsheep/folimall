package com.ysx.folimall.order.listener;

import com.rabbitmq.client.Channel;
import com.ysx.folimall.order.entity.OrderEntity;
import com.ysx.folimall.order.service.OrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "order.release.order.queue")
public class OrderCloseListener {

    @Autowired
    OrderService orderService;
    @RabbitHandler
    public void listener(OrderEntity orderEntity, Channel channel, Message mess) throws IOException {
        System.out.println(orderEntity.getOrderSn()+"订单准备关单");
        try{
            orderService.closeOrder(orderEntity);
            channel.basicAck(mess.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){

            channel.basicReject(mess.getMessageProperties().getDeliveryTag(),true);
        }
    }
}
