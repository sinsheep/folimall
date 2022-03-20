package com.ysx.folimall.auth.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Data
public class UserRegistVo {

    @NotEmpty(message = "用户名不能为空")
    private  String userName;
    @NotEmpty(message = "密码必须填写")
    private  String password;
    @Pattern(regexp = "^[1-9]([3-9])[0-9]9$",message = "手机号格式出错")
    private  String phone;
    @NotEmpty(message = "code必须填写")
    private  String code;

}
