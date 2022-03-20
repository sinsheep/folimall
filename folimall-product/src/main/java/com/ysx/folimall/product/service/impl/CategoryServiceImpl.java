package com.ysx.folimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ysx.folimall.product.service.CategoryBrandRelationService;
import com.ysx.folimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.product.dao.CategoryDao;
import com.ysx.folimall.product.entity.CategoryEntity;
import com.ysx.folimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {


    @Autowired
    private RedissonClient redisson;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    //查出所有分类组装成父子的树形结构
    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> entities = baseMapper.selectList(null);
        List<CategoryEntity> level = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildes(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO: 7/9/21 检查当前删除的菜单， 是否被别的地方引用

        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        CategoryEntity byid = this.getById(catelogId);
        while (byid.getParentCid() != 0) {
            paths.add(byid.getCatId());
            byid = this.getById(byid.getParentCid());
        }
        if (byid.getCatId() != 0)
            paths.add(byid.getCatId());
        Collections.reverse(paths);
        return paths.toArray(new Long[paths.size()]);
    }

    /**
     * 级联更新
     *
     * @param category
     */

//    @Caching(evict = {
//            @CacheEvict(value = "catagory",key="'getLevel1Categorys'"),
//            @CacheEvict(value = "catagory",key="'getCatalogJson'")
//    })
    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

    }

    @Cacheable(value = {"category"},key = "#root.method.name")

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("getLevel1Categorys.....");
        List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return entities;
    }

    @Cacheable(value = "category",key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        Map<String, Object> map = new HashMap<>();
        List<CategoryEntity> level1Categorys = getParent_cid(categoryEntities, 0L);//父类id为0的为1级分类
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> entities = getParent_cid(categoryEntities, v.getParentCid());
            List<Catelog2Vo> catelog2Vos = null;
            if (entities != null) {
                catelog2Vos = entities.stream().map(l2 -> {
                    List<CategoryEntity> level3 = getParent_cid(categoryEntities, l2.getCatId());
                    List<Catelog2Vo.Catelog3Vo> collect = null;
                    if (level3 != null) {
                        collect = level3.stream().map(l3 -> new Catelog2Vo.Catelog3Vo(
                                l2.getCatId().toString(), l3.getCatId().toString(), l3.getName()
                        )).collect(Collectors.toList());
                    }
                    return new Catelog2Vo(v.getCatId().toString(), collect, l2.getCatId().toString(), l2.getName());
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));
        return parent_cid;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (ObjectUtils.isEmpty(catalogJSON)) {
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
            return catalogJsonFromDb;
        }
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;

    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        RLock lock = redisson.getLock("catalogJson-lock");
        Map<String, List<Catelog2Vo>> dataFromDb;
        lock.lock();
        try{
            dataFromDb = getCatalogJsonFromDb();
        }finally {
            lock.unlock();
        }
        return dataFromDb;

    }
    public synchronized Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {

        /**
         * 优化将多次查询变成一次
         */
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (!ObjectUtils.isEmpty(catalogJSON)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        Map<String, Object> map = new HashMap<>();
        List<CategoryEntity> level1Categorys = getParent_cid(categoryEntities, 0L);//父类id为0的为1级分类
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> entities = getParent_cid(categoryEntities, v.getParentCid());
            List<Catelog2Vo> catelog2Vos = null;
            if (entities != null) {
                catelog2Vos = entities.stream().map(l2 -> {
                    List<CategoryEntity> level3 = getParent_cid(categoryEntities, l2.getParentCid());
                    List<Catelog2Vo.Catelog3Vo> collect = null;
                    if (level3 != null) {
                        collect = level3.stream().map(l3 -> new Catelog2Vo.Catelog3Vo(
                                l2.getCatId().toString(), l3.getCatId().toString(), l3.getName()
                        )).collect(Collectors.toList());
                    }
                    return new Catelog2Vo(v.getCatId().toString(), collect, l2.getCatId().toString(), l2.getName());
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));
        redisTemplate.opsForValue().set("catalogJSON", JSON.toJSONString(parent_cid));
        return parent_cid;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> categoryEntities, Long parentCid) {
        return categoryEntities.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }


    private List<CategoryEntity> getChildes(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> child = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid().equals(root.getCatId());
        }).map(categoryEntity -> {
            //找到子菜单
            categoryEntity.setChildren(getChildes(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return child;
    }

}