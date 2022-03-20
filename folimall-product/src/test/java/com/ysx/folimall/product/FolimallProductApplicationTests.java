package com.ysx.folimall.product;

import com.ysx.folimall.product.dao.AttrGroupDao;
import com.ysx.folimall.product.dao.SkuSaleAttrValueDao;
import com.ysx.folimall.product.entity.BrandEntity;
import com.ysx.folimall.product.service.BrandService;
import com.ysx.folimall.product.service.CategoryService;
import com.ysx.folimall.product.service.SkuSaleAttrValueService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.UUID;


@SpringBootTest
class FolimallProductApplicationTests {

	@Autowired
	BrandService brandService;

	@Autowired
	CategoryService categoryService;

	@Autowired
	StringRedisTemplate redisTemplate;

	@Autowired
	RedissonClient redissonClient;

	@Autowired
	AttrGroupDao attrGroupDao;

	@Autowired
	SkuSaleAttrValueDao dao;

	@Test
	public void test(){
//		System.out.println(attrGroupDao.getAttrGroupWithAttrsBySpuId(8L,225L));
		System.out.println(dao.getSaleAttrsBySpuId(8L));
	}


	@Test
	public void testStringRedis(){
		ValueOperations<String, String> ops = redisTemplate.opsForValue();

		ops.set("hello","word_"+ UUID.randomUUID().toString());

		System.out.println(ops.get("hello"));

	}
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
