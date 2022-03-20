package com.ysx.folimall.order.interceptor;

import com.ysx.common.constant.AuthSeverConstant;
import com.ysx.common.to.MemberRespTo;
import org.apache.shiro.util.AntPathMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespTo>  loginUser = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //order/order/status/{orderSn}
        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/status/**", uri);
        if(match){
            return true;
        }
        MemberRespTo attribute = (MemberRespTo) request.getSession().getAttribute(AuthSeverConstant.LOGIN_USER);
        if(attribute!=null){
            loginUser.set(attribute);
            return true;
        }else{
            request.getSession().setAttribute("msg","please login your account");
            response.sendRedirect("http://auth.folimall.com/login.html");
            return false;
        }
    }
}
