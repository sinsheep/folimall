package com.ysx.folimall.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {
    private Long catalog3Id;
    private String keyword;

    /**
     * sort=saleCount asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;

    private Integer hasStock=1;//只显示有货
    private String skuPrice;//价格区间
    private List<Long> brandId;//品牌进行查询可以多选
    private List<String> attrs;//属性进行筛选
    private Integer pageNum = 1;//页码
    private String _queryString;
}
