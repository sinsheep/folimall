package com.ysx.folimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ysx.common.constant.WareConstant;
import com.ysx.folimall.ware.entity.PurchaseDetailEntity;
import com.ysx.folimall.ware.service.PurchaseDetailService;
import com.ysx.folimall.ware.service.WareSkuService;
import com.ysx.folimall.ware.vo.MergeVo;
import com.ysx.folimall.ware.vo.PurchaseDoneVo;
import com.ysx.folimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.ware.dao.PurchaseDao;
import com.ysx.folimall.ware.entity.PurchaseEntity;
import com.ysx.folimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {


    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    WareSkuService wareSkuService;

    /**
     * @param params
     * @return status: 0//
     * wareId:1
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!ObjectUtils.isEmpty(key)) {

        }
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivedPurcahse(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1);

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        List<Long> items = mergeVo.getItems();
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        } else {
            // TODO: 8/10/21 handle the status of purchaseStatus without created or assigned
        }

        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(i -> {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            detailEntity.setId(i);
            detailEntity.setPurchaseId(finalPurchaseId);
            detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return detailEntity;
        }).collect(Collectors.toList());

        purchaseDetailService.updateBatchById(collect);

        PurchaseEntity entity = new PurchaseEntity();
        entity.setId(purchaseId);
        entity.setUpdateTime(new Date());
        this.updateById(entity);

    }

    /**
     * @param ids purchaseList id
     */
    @Transactional
    @Override
    public void received(List<Long> ids) {
        //确认当前采购单状态

        List<PurchaseEntity> collect = ids.stream().map(id -> {
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(item -> item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode()
                || item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode())
                .map(item -> {
                    item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
                    item.setUpdateTime(new Date());
                    return item;
                }).collect(Collectors.toList());
        // 2. changing the status of purchase list

        this.updateBatchById(collect);
        //3. changing status of purchase item

        collect.forEach(item -> {
            List<Long> detailIds = purchaseDetailService.listDetailByPurchaseId(item.getId()).stream().map(i -> i.getId()).collect(Collectors.toList());
            purchaseDetailService.update(new UpdateWrapper<PurchaseDetailEntity>().in("id", detailIds).set("status", WareConstant.PurchaseDetailStatusEnum.BUYING.getCode()));
        });
    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {


        Long id = doneVo.getId();

        //change status of purchase item
        List<PurchaseItemDoneVo> items = doneVo.getItems();

        List<PurchaseDetailEntity> updates = new ArrayList<>();
        Boolean flag = true;
        for (PurchaseItemDoneVo item: items){
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if(item.getStatus()==WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                flag = false;
                detailEntity.setStatus(item.getStatus());
            }else{
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                // add item to ware
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum());
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);

            purchaseDetailService.updateBatchById(updates);
        }

        //change  status of purchase menu
        PurchaseEntity purchaseEntity = new PurchaseEntity();

        purchaseEntity.setId(id);
        purchaseEntity.setStatus(flag?WareConstant.PurchaseDetailStatusEnum.FINISH.getCode()
                : WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

}