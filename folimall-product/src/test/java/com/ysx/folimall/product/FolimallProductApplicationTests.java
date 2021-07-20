package com.ysx.folimall.product;

import com.ysx.folimall.product.entity.BrandEntity;
import com.ysx.folimall.product.service.BrandService;
import com.ysx.folimall.product.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;


@SpringBootTest
class FolimallProductApplicationTests {

	@Autowired
	BrandService brandService;

	@Autowired
	CategoryService categoryService;


	@Test
	public  void testFindPath(){
		Long[] catelogPath = categoryService.findCatelogPath(230L);
		System.out.println(Arrays.asList(catelogPath));
	}
	@Test
	void contextLoads() {
		BrandEntity brandEntity = new BrandEntity();
		brandEntity.setBrandId(1L);
		brandEntity.setName("apple");
		brandService.updateById(brandEntity);
		System.out.println("保存成功");
	}

}
