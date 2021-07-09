package com.ysx.folimall.product.dao;

import com.ysx.folimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 19:19:13
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
