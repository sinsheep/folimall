package com.ysx.folimall.order.vo;

import lombok.Data;

import java.lang.reflect.Member;
import java.math.BigDecimal;

@Data
public class FareVo {
    private MemberAddressVo address;
    private BigDecimal fare;
}
