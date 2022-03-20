package com.ysx.folimall.search.vo;

import com.ysx.common.to.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {

    private List<SkuEsModel> products;
    private Integer pageNum;
    private Long total;//总记录数
    private List<Integer> pageNavs;//
    private Integer totalPages;//总页数
    private  List<BrandVo> brands;//当前所有涉及到的品牌
    private List<AttrVo> attrs;//所有属性


    private List<NavVo>  navs = new ArrayList<>();//面包屑导航

    private List<CatalogVo> catalogs;//当前涉及的所有vo


    @Data
    public static class NavVo{
        private String navName;
        private String navValue;
        private String link;
    }
    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class CatalogVo{
        private Long catalogId;
        private String catalogName;
    }
    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
}
