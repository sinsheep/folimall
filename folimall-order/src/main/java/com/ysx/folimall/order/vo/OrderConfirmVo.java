package com.ysx.folimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class OrderConfirmVo {

    @Getter @Setter
    List<MemberAddressVo> address;

    //item of shop
    @Getter @Setter
    List<OrderItemVo> items;

    //coupon
    @Getter @Setter
    private Integer integration;

    BigDecimal total;

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if(items!=null){
            for (OrderItemVo item : items) {
                sum = sum.add(item.getPrice().multiply(BigDecimal.valueOf(item.getCount())));
            }
        }
        return sum;
    }

    BigDecimal payPrice;

    public BigDecimal getPayPrice() {

        BigDecimal sum = new BigDecimal("0");
        if(items!=null){
            for (OrderItemVo item : items) {
                sum = sum.add(item.getPrice().multiply(BigDecimal.valueOf(item.getCount())));
            }
        }
        return sum;
    }

    public Integer getCount() {
        Integer i = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }

    //防重令牌
    @Setter @Getter
    String orderToken;

    @Setter
    @Getter
    Map<Long, Boolean> stocks;
}
