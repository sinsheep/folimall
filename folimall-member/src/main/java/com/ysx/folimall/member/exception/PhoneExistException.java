package com.ysx.folimall.member.exception;

public class PhoneExistException extends RuntimeException{

    public PhoneExistException(){
        super("手机已经存在");
    }
}

