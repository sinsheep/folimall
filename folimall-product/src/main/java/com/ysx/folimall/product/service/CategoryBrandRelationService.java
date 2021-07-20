package com.ysx.folimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ysx.common.utils.PageUtils;
import com.ysx.folimall.product.entity.CategoryBrandRelationEntity;

import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 19:19:13
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    void updateBrand(Long brandId, String name);

    void updateCategory(Long catId, String name);
}

