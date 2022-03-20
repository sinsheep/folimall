package com.ysx.folimall.product.service.impl;

import com.ysx.folimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.product.dao.BrandDao;
import com.ysx.folimall.product.entity.BrandEntity;
import com.ysx.folimall.product.service.BrandService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");

        QueryWrapper<BrandEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("brand_id",key).or().like("name",key);
        }
        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void updateDetail(BrandEntity brand) {
        //保证冗余字段的一致性
        this.updateById(brand);
        if (!StringUtils.containsWhitespace((brand.getName()))) {

            //同步更新其他表中的数据
            categoryBrandRelationService.updateBrand(brand.getBrandId(),brand.getName());
            // TODO: 7/20/21 更新其他关联
        }


    }

    @Override
    public List<BrandEntity> getBrandsByIds(List<Long> brandIds) {
        baseMapper.selectList(new QueryWrapper<BrandEntity>().in("brand_id",brandIds));
        return null;
    }

}