package com.ysx.folimall.member.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Data
public class MemberRegistVo {

    private  String userName;
    private  String password;
    private  String phone;
}
