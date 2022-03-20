package com.ysx.folimall.ware.vo;

import lombok.Data;


@Data
public class LockStockResult {
    private Long skuId;
    private Integer num;
    private boolean locked;
}
