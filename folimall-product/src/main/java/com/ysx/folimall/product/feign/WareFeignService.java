package com.ysx.folimall.product.feign;

import com.ysx.common.to.SkuHasStockTo;
import com.ysx.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("folimall-ware")
public interface WareFeignService {


    @PostMapping("/ware/waresku/hasstock")
    public R getSkusHasStock(@RequestBody List<Long> skuIds);

}
