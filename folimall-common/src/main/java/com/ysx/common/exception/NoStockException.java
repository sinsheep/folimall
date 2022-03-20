package com.ysx.common.exception;

public class NoStockException extends RuntimeException{

    private Long skuId;
    public NoStockException(Long skuId){
        super("商品["+skuId+"]没有足够的内存");
    }
    public NoStockException(){
        super("没有足够的内存");
    }
}
