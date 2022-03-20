package com.ysx.common.exception;


/**
 * 11: item
 * 12: order
 * 13: purchase
 * 14: transport
 * 15: user
 * 21: ware
 */
public enum BizCodeEnume {

    VALID_EXCEPTION(10001,"参数校验失败"),
    UNKNOW_EXCEPTION(10000,"未知错误"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常"),
    USER_EXIST_EXCEPTION(15001,"用户存在"),
    PHONE_EXIST_EXCEPTION(15002,"手机号存在"),
    LOGINACCT_PASSWORD_INVALID_EXCEPTION(15003,"帐号或者密码错误"),
    NO_STOCK_EXCEPTION(21000,"商品库存不住"),
    SMS_CODE_EXCEPTION(10002,"信息发送失败");

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
