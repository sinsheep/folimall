package com.ysx.folimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ysx.common.to.SkuEsModel;
import com.ysx.common.utils.R;
import com.ysx.folimall.search.config.FolimallElasticSearchConfig;
import com.ysx.folimall.search.constant.EsConstant;
import com.ysx.folimall.search.feign.ProductFeignService;
import com.ysx.folimall.search.service.MallSearchService;
import com.ysx.folimall.search.vo.AttrResponseVo;
import com.ysx.folimall.search.vo.BrandVo;
import com.ysx.folimall.search.vo.SearchParam;
import com.ysx.folimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    RestHighLevelClient client;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam param) {

        SearchResult result = null;
        //1.准备检索请求
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            //2执行检索请求
            SearchResponse response = client.search(searchRequest, FolimallElasticSearchConfig.COMMON_OPTIONS);
            //3.分装成响应数据
            result = buildSearchResult(response, param);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result = new SearchResult();
        //返回查询到的商品
        SearchHits hits = response.getHits();

        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel sku = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!ObjectUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String skuTitleStr = skuTitle.getFragments()[0].string();
                    sku.setSkuTitle(skuTitleStr);
                }
                esModels.add(sku);
            }
        }
        result.setProducts(esModels);

        //所有商品的属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");

        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();

            //attr_id
            long attrId = bucket.getKeyAsNumber().longValue();
            //attr_name
            String attr_name = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            //attr_value
            List<String> attrValue = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());


            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attr_name);
            attrVo.setAttrValue(attrValue);
            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        //所有商品的品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //brand_id
            String keyAsString = bucket.getKeyAsString();

            //brand_name
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();

            //brand_img
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();

            brandVo.setBrandImg(brandImg);
            brandVo.setBrandName(brandName);
            brandVo.setBrandId(Long.parseLong(keyAsString));
            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);

        //设置分类信息
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        for (Terms.Bucket bucket : catalog_agg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            String keyAsString = bucket.getKeyAsString();
            //取子聚合
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();

            catalogVo.setCatalogName(catalogName);
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //分页信息-页码
        result.setPageNum(param.getPageNum());
        //分页信息-总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        //分页信息-总页码
        int pages = (int) (total / EsConstant.PRODUCT_PAGESIZE);
        int totalPages = (int) total % EsConstant.PRODUCT_PAGESIZE == 0 ? pages : pages + 1;
        result.setTotalPages(totalPages);


        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);


        //构建面包屑导航功能
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {


            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.getAttrsInfo(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData(new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                } else {
                    navVo.setNavName(s[0]);
                }
                //取消面包屑后 把当前的条件制空
                String replace = replaceQueryString(param, attr,"attrs");

                navVo.setLink("http://search.folimall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVos);
        }
        if ((param.getBrandId()!=null && param.getBrandId().size()>0)){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            R brandsInfo = productFeignService.getBrandsInfo(param.getBrandId());
            if(brandsInfo.getCode()==0){
                List<BrandVo> data = brandsInfo.getData(new TypeReference<List<BrandVo>>() {
                });
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandVo brand : data) {
                    buffer.append(brand.getBrandName());
                    replace = replaceQueryString(param,brand.getBrandId().toString(),"brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink(replace);
            }
            navs.add(navVo);
        }
        //TODO 分类navs 面包屑
        return result;
    }

    private String replaceQueryString(SearchParam param, String value,String key) {
        String encode = null;
        try{
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+","%20");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        String replace = param.get_queryString().replace("&"+key+"=" + encode, "");
        return replace;
    }

    private SearchRequest buildSearchRequest(SearchParam param) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /**
         * 查询模糊匹配，过滤按照属性，分类、品牌、价格区间、推荐、库存信息
         */
        //构建boolquery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1 must匹配
        if (!ObjectUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        //1.2 filter  按照三级分类id查询
        if (!ObjectUtils.isEmpty(param.getCatalog3Id())) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }

        //1.2 filter 按照brandId
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        //1.4 filter 按照所有属性查询

        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {

                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                String attrId = s[0];//id
                String[] attrValues = s[1].split(":");//属性值

                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        //1.5 按照有无库存查询
        boolQuery.filter(QueryBuilders.termsQuery("hasStock", param.getHasStock() == 1));


        //1.6按照价格区间查询
        if (!ObjectUtils.isEmpty(param.getSkuPrice())) {
            //1_500/_500/500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");

            String[] s = param.getSkuPrice().split("_");
            if (param.getSkuPrice().startsWith("_")) {
                rangeQuery.lte(s[1]);
            } else if (param.getSkuPrice().endsWith("_")) {
                rangeQuery.gte(s[1]);
            } else {
                rangeQuery.gte(s[0]).lte(s[1]);
            }


            boolQuery.filter(rangeQuery);
        }
        sourceBuilder.query(boolQuery);

        /**
         * 查询排序,分页，高亮
         */
        //2.1 排序
        if (!ObjectUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] s = sort.split("_");
            sourceBuilder.sort(s[0], SortOrder.fromString(s[1]));
        }


        //2.2 分页
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //2.3highlight
        if (!ObjectUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }
        //2.4 aggregation
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg");
        brandAgg.field("brandId").size(50);
        //品牌聚合 catalog_agg
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brandAgg);

        // 分类聚合 catalog_agg
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalogAgg);
        //属性聚合 attr_agg

        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
        //聚合处当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");

        //聚合处当前所有的attrId 对应的name
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //聚合处当前所有的attrId所有可能的属性值
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));

        attrAgg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attrAgg);
        System.out.println(sourceBuilder.toString());
        SearchRequest searchRequest = new SearchRequest(new String[]{
                EsConstant.PRODUCT_INDEX
        }, sourceBuilder);
        return searchRequest;
    }
}
