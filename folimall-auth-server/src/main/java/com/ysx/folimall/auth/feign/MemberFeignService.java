package com.ysx.folimall.auth.feign;

import com.ysx.common.utils.R;
import com.ysx.folimall.auth.vo.UserLoginVo;
import com.ysx.folimall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("folimall-member")
public interface MemberFeignService {
    @PostMapping("member/member/register")
    R register(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);
}
