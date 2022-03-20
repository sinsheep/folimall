package com.ysx.folimall.cart.service;

import com.ysx.folimall.cart.vo.Cart;
import com.ysx.folimall.cart.vo.CartItem;

import java.util.List;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num);

    CartItem getCartItem(Long skuId);

    Cart getCart();
    void clearCart(String cartKey);

    /**
     * 勾选商品
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    void changeItemCount(Long skuId, Integer num);

    void deleteItem(Long skuId);

    List<CartItem> getUserCartItems();
}
