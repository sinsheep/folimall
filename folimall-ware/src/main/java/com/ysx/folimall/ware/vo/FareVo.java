package com.ysx.folimall.ware.vo;

import lombok.Data;

import javax.xml.stream.events.Characters;
import java.math.BigDecimal;

@Data
public class FareVo {

    private MemberAddressVo address;
    private BigDecimal fare;
}
