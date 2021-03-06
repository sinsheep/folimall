package com.ysx.folimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.ysx.common.constant.ProductConstant;
import com.ysx.common.to.SkuEsModel;
import com.ysx.common.to.SkuHasStockTo;
import com.ysx.common.to.SkuReductionTo;
import com.ysx.common.to.SpuBoundTo;
import com.ysx.common.utils.R;
import com.ysx.folimall.product.entity.*;
import com.ysx.folimall.product.feign.CouponFeignService;
import com.ysx.folimall.product.feign.SearchFeignService;
import com.ysx.folimall.product.feign.WareFeignService;
import com.ysx.folimall.product.service.*;
import com.ysx.folimall.product.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SearchFeignService searchFeignService;
    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    SpuInfoDescServiceImpl spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService imagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    WareFeignService wareFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * //TODO ??????????????????
     *
     * @param vo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1 ??????spu???????????? pms_spu_info

        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();

        BeanUtils.copyProperties(vo, spuInfoEntity);

        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());

        this.saveBaseSpuInfo(spuInfoEntity);

        // 2.??????spu????????????pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity infoDescEntity = new SpuInfoDescEntity();
        infoDescEntity.setSpuId(spuInfoEntity.getId());
        infoDescEntity.setDecript(String.join(",", decript));
        spuInfoDescService.saveSpuInfoDesc(infoDescEntity);

        //3. ??????spu???????????? pms_spu_images
        List<String> images = vo.getImages();

        spuImagesService.saveImages(spuInfoEntity.getId(), images);

        //4. ??????spu??????????????? pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setAttrId(attr.getAttrId());

            AttrEntity byId = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(byId.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            valueEntity.setSpuId(spuInfoEntity.getId());
            return valueEntity;
        }).collect(Collectors.toList());

        productAttrValueService.saveProductAttr(collect);
        //5. ??????spu??????????????? folimall_sms-> sms_spu_bounds

        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("????????????spu??????????????????");
        }
        //6.????????????spu?????????sku??????
        //6.1 sku ???????????? pms_sku_info
        List<Skus> skus = vo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach(sku -> {
                String defaultImg = "";
                for (Images img : sku.getImages()) {
                    if (img.getDefaultImg() == 1) {
                        defaultImg = img.getImgUrl();
                    }
                }

                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);

                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);

                //6.2 ??????sku???????????????pms_sku_images
                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> imagesEntities = sku.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity -> !ObjectUtils.isEmpty(entity.getImgUrl())).collect(Collectors.toList());

                // TODO  ??????sku??????????????????????????????
                imagesService.saveBatch(imagesEntities);

                //6.3 sku????????????????????? pms-sku_sale_attr_value
                List<Attr> attrs = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attrs.stream().map(attr -> {
                    SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, attrValueEntity);
                    attrValueEntity.setSkuId(skuId);
                    return attrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);
                //6.4 sku ????????????????????? folimall sms_sku_ladder/sms_sku_full_reduction/sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(BigDecimal.ZERO) == 1) {

                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);

                    if (r1.getCode() != 0) {
                        log.error("????????????sku??????????????????");
                    }
                }


            });
        }
    }


    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {

        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {


        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!ObjectUtils.isEmpty(key)) {
            wrapper.and(w ->
                    w.eq("id", key).or().like("spu_name", key)
            );

        }
        String status = (String) params.get("status");
        if (!ObjectUtils.isEmpty(status)) {
            wrapper.eq("publish_status", status);

        }
        String brandId = (String) params.get("brandId");
        if (!ObjectUtils.isEmpty(brandId) && !"0".equals(brandId)) {
            wrapper.eq("brand_id", brandId);

        }
        String catalogId = (String) params.get("catalogId");
        if (!ObjectUtils.isEmpty(catalogId) && !"0".equals(catalogId)) {
            wrapper.eq("catalog_id", catalogId);

        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void up(long spuId) {

//        List<SkuEsModel> uoProducts = new ArrayList<>();
        // ????????????sku?????????spu?????????????????????
        List<SkuInfoEntity> skuInfoEntities = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIdList = skuInfoEntities.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        // TODO: 8/29/21 ??????sku??????????????????????????????????????????

        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrlistForSpu(spuId);

        List<Long> attrIds = baseAttrs.stream().map(attr -> attr.getAttrId()).collect(Collectors.toList());
        List<Long> searchAttrIds = attrService.selectSearchAttrs(attrIds);

        Set<Long> idSet = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attr> attrList = baseAttrs.stream().filter(item -> idSet.contains(item.getAttrId()))
                .map(item -> {
                    SkuEsModel.Attr attr = new SkuEsModel.Attr();
                    BeanUtils.copyProperties(item, attr);
                    ;
                    return attr;
                }).collect(Collectors.toList());
        Map<Long, Boolean> stockMap = null;
        // TODO: 8/29/21 ?????????????????? ???????????????????????????
        try {

            R skusHasStock = wareFeignService.getSkusHasStock(skuIdList);

            stockMap = skusHasStock.getData(new TypeReference<List<SkuHasStockTo>>(){})
                    .stream().collect(Collectors.toMap(SkuHasStockTo::getSkuId, item -> item.getHasStock()));
        } catch (Exception e) {
            log.error("????????????????????????????????????", e);
        }

        //????????????sku??????
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skuInfoEntities.stream().map(sku -> {
            //?????????????????????
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, esModel);
            // different: skuPrice SkuImg hasStock hotScore
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            if (finalStockMap == null) {
                esModel.setHasStock(true);

            } else {
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
            // TODO: 8/29/21 ???????????? default0
            esModel.setHotScore(0L);
            //??????????????????????????????
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());

            //??????????????????
            esModel.setAttrs(attrList);

            return esModel;
        }).collect(Collectors.toList());
        List<SkuEsModel> esModels = upProducts;

        //?????????es
        R r = searchFeignService.productStatusUp(upProducts);
        if (r.getCode() == 0) {
            //successful
            // TODO: 8/31/21 ??????spu?????????
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        } else {
            //failure
            // TODO: 8/31/21 ??????????????? ???????????? ???????????????
        }


    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        SkuInfoEntity byId = skuInfoService.getById(skuId);
        Long spuId = byId.getSpuId();
        SpuInfoEntity spuInfoEntity = getById(spuId);
        return spuInfoEntity;
    }

}