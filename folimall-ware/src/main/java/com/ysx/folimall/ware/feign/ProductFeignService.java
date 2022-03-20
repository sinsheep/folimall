package com.ysx.folimall.ware.feign;

import com.ysx.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("folimall-product")
public interface ProductFeignService {

    /**
     * /product/skuinfo/info/{skuId}
     * api/product/skuinfo/info/{skuId}
     *
     * 1. 让请求过网关:
     *    1 @FeignClient("folimall-gateway") 给folimall-gateway 所在的机器发请求
     * api/product/skuinfo/info/{skuId}
     *    2. 直接让后台指定处理 @FeignClient("folimall-product")
     * /product/skuinfo/info/{skuId}
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R info(@PathVariable("skuId") Long skuId);
}
