/**
  * Copyright 2021 json.cn 
  */
package com.ysx.folimall.product.vo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import com.ysx.common.to.MemberPrice;

/**
 * Auto-generated: 2021-07-27 23:53:55
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class Skus {

    private List<Attr> attr;
    private String skuName;
    private BigDecimal price;
    private String skuTitle;
    private String skuSubtitle;
    private List<Images> images;
    private List<String> descar;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    private List<MemberPrice> memberPrice;
}