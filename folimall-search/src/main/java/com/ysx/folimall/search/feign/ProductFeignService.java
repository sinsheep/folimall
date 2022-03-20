package com.ysx.folimall.search.feign;

import com.ysx.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("folimall-product")
public interface ProductFeignService {
    @GetMapping("product/attr/info/{attrId}")
    R getAttrsInfo(@PathVariable("attrId") Long attrId);

    @RequestMapping("product/brand/infos")
    R getBrandsInfo(@RequestParam("attrId") List<Long> attrId);
}
