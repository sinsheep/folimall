package com.ysx.folimall.order.feign;

import com.ysx.folimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("folimall-cart")
public interface CartFeignService {

    @GetMapping("currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();
}
