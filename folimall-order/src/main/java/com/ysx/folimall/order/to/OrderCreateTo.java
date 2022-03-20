package com.ysx.folimall.order.to;

import com.ysx.folimall.order.entity.OrderEntity;
import com.ysx.folimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice;

    private BigDecimal fare;//运费
}
