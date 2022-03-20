package com.ysx.folimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {

    private Long addrId;
    private Integer payType;
    //无需提交购买的商品，从购物车获取

    private String orderToken;
    private BigDecimal payPrice;
    private String note;

}
