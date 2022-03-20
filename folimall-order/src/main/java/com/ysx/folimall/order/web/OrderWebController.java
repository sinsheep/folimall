package com.ysx.folimall.order.web;

import com.ysx.folimall.order.entity.OrderEntity;
import com.ysx.folimall.order.service.OrderService;
import com.ysx.folimall.order.vo.OrderConfirmVo;
import com.ysx.folimall.order.vo.OrderSubmitVo;
import com.ysx.folimall.order.vo.SubmitOrderResponseVo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/test/createOrder")
    @ResponseBody
    public String createOrderTest(){
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",orderEntity);
        return "ok";
    }
    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData",confirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
        if(responseVo.getCode() == 0){
            model.addAttribute("submitOrderResp",responseVo);
            return "pay";
        }else{
            String msg="下单失败:";
            switch (responseVo.getCode()){
                case 1:
                    msg+="订单信息过期，请重新刷新订单";
                    break;
                case 2:
                    msg+="订单商品价格发生变化，请确定价格后再次提交";
                    break;
                case 3:
                    msg+="库存不足，库存锁定失败";
                    break;
            }
            redirectAttributes.addFlashAttribute("msg",msg);
            return "redirect:http://order.folimall.com/toTrade";
        }
    }
}
