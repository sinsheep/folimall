package com.ysx.common.exception;


public enum BizCodeEnume {

    VALID_EXCEPTION(10001,"参数校验失败"),
    UNKNOW_EXCEPTION(10000,"未知错误");

    private Integer code;
    private String msg;

    BizCodeEnume(Integer code,String msg){
        this.code = code;
        this.msg = msg;
    }


    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
