package com.ysx.folimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ysx.common.utils.PageUtils;
import com.ysx.folimall.product.entity.SpuCommentEntity;

import java.util.Map;

/**
 * 商品评价
 *
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 19:19:13
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

