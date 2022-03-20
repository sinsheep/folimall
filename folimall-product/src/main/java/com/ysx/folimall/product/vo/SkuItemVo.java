package com.ysx.folimall.product.vo;

import com.ysx.folimall.product.entity.SkuImagesEntity;
import com.ysx.folimall.product.entity.SkuInfoEntity;
import com.ysx.folimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {

    //sku基本信息
    SkuInfoEntity info;

    // images of sku
    List<SkuImagesEntity> images;

    //spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;

    //spu 介绍
    SpuInfoDescEntity desc;

    //获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;

    //hasstock
    Boolean hasStock = true;

    //秒杀
    SeckillInfoVo seckillInfoVo;
    @Data
    public static class SkuItemSaleAttrVo {
        private Long attrId;
        private String attrName;
        private List<AttrValueWithSkuIdVo> attrValues;
    }

    @ToString
    @Data
    public static class SpuItemAttrGroupVo {
        private String groupName;
        private List<SpuBaseAttrVo> attrs;
    }

    @Data
    public static class SpuBaseAttrVo {
        private String attrName;
        private String attrValue;
    }

    @Data
    public static class AttrValueWithSkuIdVo {
        private String attrValue;
        private String skuIds;
    }


}
