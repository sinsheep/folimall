package com.ysx.folimall.cart.controller;

import com.ysx.folimall.cart.service.CartService;
import com.ysx.folimall.cart.vo.Cart;
import com.ysx.folimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
public class CartController {

    @Autowired
    CartService cartService;
    /**
     * 登录用session
     * 没登录用cookie带来的user-key
     *
     * @param
     * @return
     */

    @ResponseBody
    @GetMapping("currentUserCartItems")
    public List<CartItem> getCurrentUserCartItems(){
        return cartService.getUserCartItems();
    }

    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId
            , @RequestParam("num") Integer num, RedirectAttributes redirectAttributes) {
        cartService.addToCart(skuId,num);
        redirectAttributes.addAttribute("skuId",skuId);
        return "redirect:http://cart.folimall.com/addToCartSuccess.html";
    }

    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,Model model){
        //CartInterceptor.threadLocal.get();
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("cartItem",cartItem);
        return "success";
    }

    @GetMapping("/cart.html")
    public String cartListPage(Model model){
        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId
            ,@RequestParam("num") Integer num){
        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.folimall.com/cart.html";
    }
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId
            ,@RequestParam("checked") Integer checked){

        cartService.checkItem(skuId,checked);

        return "redirect:http://cart.folimall.com/cart.html";
    }

    @GetMapping("deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.folimall.com/cart.html";
    }
}
