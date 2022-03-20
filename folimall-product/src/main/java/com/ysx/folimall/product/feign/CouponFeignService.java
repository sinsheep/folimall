package com.ysx.folimall.product.feign;

import com.ysx.common.to.SkuReductionTo;
import com.ysx.common.to.SpuBoundTo;
import com.ysx.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("folimall-coupon")
public interface CouponFeignService {


    /**
     * couponFeignService.saveSpuBounds(sputBoundTO)
     * 1. @RequestBody 将这个对象转为json
     * 2. 找到folimall-coupon对象 给/coupon/spubounds/save 发请求
     * 将上一步转的json放在请求体的位置，发送请求
     * 3. 对方服务受到请求，请求体里有json数据
     * function(@RequestBody SpuBoundsEntity spuBounds) 将请求体的json转为SpuBoundsEntity
     * 只要json的数据模型数据模型是兼容的，双方服务无需使用同一个to，属性名一一对应就ok
     * @param spuBoundTo
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);
    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody  SkuReductionTo skuReductionTo);
}
