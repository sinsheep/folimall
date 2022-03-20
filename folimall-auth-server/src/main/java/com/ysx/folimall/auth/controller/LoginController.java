package com.ysx.folimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.ysx.common.constant.AuthSeverConstant;
import com.ysx.common.exception.BizCodeEnume;
import com.ysx.common.to.MemberRespTo;
import com.ysx.common.utils.R;
import com.ysx.folimall.auth.feign.MemberFeignService;
import com.ysx.folimall.auth.feign.ThirdPartFeignService;
import com.ysx.folimall.auth.vo.UserLoginVo;
import com.ysx.folimall.auth.vo.UserRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {


    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {

        String redisCode = redisTemplate.opsForValue().get(AuthSeverConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (redisCode != null) {
            long i = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - i < 60000) {
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        String code = UUID.randomUUID().toString().substring(0, 5)+"_"+System.currentTimeMillis();
        thirdPartFeignService.sendCode(phone, code.split("_")[0]);
        redisTemplate.opsForValue().set(AuthSeverConstant.SMS_CODE_CACHE_PREFIX + phone, code, 10, TimeUnit.MINUTES);
        return R.ok();
    }

    @PostMapping("/register")
    public String register(@Valid UserRegistVo userVo, BindingResult result, RedirectAttributes model) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().stream().collect(Collectors.toMap(fieldError ->
                            fieldError
                    , fieldError -> fieldError.getDefaultMessage()));
            model.addFlashAttribute("errors",errors);
            return "redirect:http://auth.folimall.com/reg.html";
        }
        String code = userVo.getCode();
        String s = redisTemplate.opsForValue().get(AuthSeverConstant.SMS_CODE_CACHE_PREFIX + userVo.getPhone());
        if(!StringUtils.isEmpty(code)){
//            System.out.println(code +" "+ s);
            if(code.equals(s.split("_")[0])){

                redisTemplate.delete(AuthSeverConstant.SMS_CODE_CACHE_PREFIX+userVo.getPhone());
                R r = memberFeignService.register(userVo);
                System.out.println(r.getCode() );
                if(r.getCode()==0){
                    return "redirect:http://auth.folimall.com/login.html";
                }else{
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg",r.getData(new TypeReference<String>(){}));
                    model.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.folimall.com/reg.html";
                }
            }else{
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                model.addFlashAttribute("errors",errors);
                return "redirect:http://auth.folimall.com/reg.html";
            }
        }else{
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            model.addFlashAttribute("errors",errors);
            return "redirect:http://auth.folimall.com/reg.html";
        }
    }
    public String loginPage(HttpSession session){
       if(session.getAttribute(AuthSeverConstant.LOGIN_USER)==null) {
           return "login";
       }else{
           return "redirect:http://folimall.com";
       }

    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){

        R login = memberFeignService.login(vo);
        if(login.getCode()==0){
            MemberRespTo data = login.getData("data", new TypeReference<MemberRespTo>() {
            });
            session.setAttribute(AuthSeverConstant.LOGIN_USER,data);
            System.out.println(data);
            return "redirect:http://folimall.com";
        }else{
            Map<String,String> errors = new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://folimall.com/login.html";
        }
    }
    @GetMapping({"/login","/login.html"})
    public String loginPage(){
        return "login";
    }
}
