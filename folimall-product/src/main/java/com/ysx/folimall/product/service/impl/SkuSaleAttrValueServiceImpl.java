package com.ysx.folimall.product.service.impl;

import com.ysx.folimall.product.vo.SkuItemVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.product.dao.SkuSaleAttrValueDao;
import com.ysx.folimall.product.entity.SkuSaleAttrValueEntity;
import com.ysx.folimall.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuItemVo.SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId) {
        List<SkuItemVo.SkuItemSaleAttrVo> vos = this.baseMapper.getSaleAttrsBySpuId(spuId);

        return vos;
    }

    @Override
    public List<String> getSkuSaleAttrValuesAsList(Long skuId) {
        SkuSaleAttrValueDao dao = this.baseMapper;

        return dao.getSkuSaleAttrValuesAsList(skuId);
    }

}