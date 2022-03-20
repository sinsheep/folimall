package com.ysx.folimall.product.web;

import com.ysx.folimall.product.entity.CategoryEntity;
import com.ysx.folimall.product.service.CategoryService;
import com.ysx.folimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {

        System.out.println(""+Thread.currentThread().getId());
        List<CategoryEntity> categoryEntity = categoryService.getLevel1Categorys();

        model.addAttribute("categorys", categoryEntity);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> catalogJson =  categoryService.getCatalogJson();

        return catalogJson;
    }
}
