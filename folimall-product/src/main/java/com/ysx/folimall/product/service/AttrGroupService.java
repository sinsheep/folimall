package com.ysx.folimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ysx.common.utils.PageUtils;
import com.ysx.folimall.product.entity.AttrGroupEntity;

import java.util.Map;

/**
 * 属性分组
 *
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 19:19:13
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);
}

