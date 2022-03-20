package com.ysx.folimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.ysx.common.to.OrderTo;
import com.ysx.common.to.mq.StockDetailTo;
import com.ysx.common.to.mq.StockLockedTo;
import com.ysx.common.utils.R;
import com.ysx.folimall.ware.entity.WareOrderTaskDetailEntity;
import com.ysx.folimall.ware.entity.WareOrderTaskEntity;
import com.ysx.folimall.ware.service.WareSkuService;
import com.ysx.folimall.ware.vo.OrderVo;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息");
        try{
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭请求");

        try{

            wareSkuService.unlockStock(orderTo);
        }catch (Exception e){

            channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
        }
    }
}
