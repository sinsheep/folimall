package com.ysx.folimall.order.vo;

import com.ysx.folimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {
    private OrderEntity orderEntity;
    private Integer code;//0 success, other failure
}
