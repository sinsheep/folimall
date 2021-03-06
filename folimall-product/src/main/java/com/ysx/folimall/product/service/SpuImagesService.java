package com.ysx.folimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ysx.common.utils.PageUtils;
import com.ysx.folimall.product.entity.SpuImagesEntity;

import java.util.List;
import java.util.Map;

/**
 * spu图片
 *
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 19:19:13
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveImages(Long id, List<String> images);
}

