package com.ysx.folimall.ware.feign;

import com.ysx.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("folimall-member")
public interface MemberFeignService {
    @RequestMapping("member/memberreceiveaddress/info/{id}")
    R addrInfo(@PathVariable("id") Long id);

}
