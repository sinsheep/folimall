package com.ysx.folimall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class Cart {

    private List<CartItem> items;
    private Integer countNum;
    private Integer countType;
    private BigDecimal totalAmount;
    private BigDecimal reduce = new BigDecimal("0");

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        countNum = 0;
        if(items!=null&& items.size()>0){

            items.forEach(item->{
                countNum += item.getCount();
            });
        }
        return countNum;
    }

    public Integer getCountType() {
        countType = 0;
        if(items!=null&& items.size()>0){

            items.forEach(item->{
                countNum += 1;
            });
        }
        return countType;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        if(items!=null&& items.size()>0){
            for (CartItem item : items) {
                if(item.getCheck()){
                    BigDecimal totalPrice = item.getTotalPrice();
                    amount = amount.add(totalPrice);
                }
            }
        }
//        2.减去优惠总价
        amount = amount.subtract(reduce);
        return amount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
