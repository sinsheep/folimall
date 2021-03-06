package com.ysx.folimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.ysx.common.to.OrderTo;
import com.ysx.common.to.mq.StockDetailTo;
import com.ysx.common.to.mq.StockLockedTo;
import com.ysx.common.utils.R;
import com.ysx.common.exception.NoStockException;
import com.ysx.folimall.ware.entity.WareOrderTaskDetailEntity;
import com.ysx.folimall.ware.entity.WareOrderTaskEntity;
import com.ysx.folimall.ware.feign.OrderFeignService;
import com.ysx.folimall.ware.feign.ProductFeignService;
import com.ysx.common.to.SkuHasStockTo;
import com.ysx.folimall.ware.service.WareOrderTaskDetailService;
import com.ysx.folimall.ware.service.WareOrderTaskService;
import com.ysx.folimall.ware.vo.OrderItemVo;
import com.ysx.folimall.ware.vo.OrderVo;
import com.ysx.folimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.ware.dao.WareSkuDao;
import com.ysx.folimall.ware.entity.WareSkuEntity;
import com.ysx.folimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareOrderTaskService orderTaskService;

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;


    public void unlockStock(Long skuId, long wareId, Integer num, Long taskDetailId) {
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);
        orderTaskDetailService.updateById(entity);
        baseMapper.unlockStock(skuId, wareId, num);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if (!ObjectUtils.isEmpty(skuId)) {
            query().eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!ObjectUtils.isEmpty(wareId)) {
            query().eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // if doesn't have this item in ware

        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList((new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId)));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {

            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setStock(skuNum);
            skuEntity.setSkuId(skuId);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            // if failure doesn't need to rollback
            // TODO: 8/12/21 ??????????????????????????????????????? ???????????????
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            }
            this.baseMapper.insert(skuEntity);
        } else {

            this.baseMapper.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockTo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockTo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockTo vo = new SkuHasStockTo();

            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;
    }
//    rollbackFor = NoStockException.class

    /**
     * ????????????
     * ????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param vo
     * @return
     */
    @Override
    @Transactional
    public boolean orderLockStock(WareSkuLockVo vo) {

        /**
         * ?????????????????????
         */

        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);
        // ??????????????????????????????
        //??????????????????????????????????????????
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            List<Long> wareIds = this.baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareIds(wareIds);
            return stock;
        }).collect(Collectors.toList());

        //????????????
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareIds();

            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                Long count = baseMapper.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    skuStocked = true;
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(),
                            taskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(taskDetailEntity);
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, detailTo);
                    lockedTo.setDetail(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                }
            }
            if (skuStocked == false) {
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {


        System.out.println("??????????????????id");
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();
        //????????????????????????????????????
        //??? ??????????????????
        // 1.?????????????????????????????????
        // 2. ????????????????????????????????????
        //    1. ?????? ????????????
        //    2. ????????? ??????????????????
        //????????????????????????????????????????????????????????????????????????
        WareOrderTaskDetailEntity detailEntity = orderTaskDetailService.getById(detailId);
        if (detailEntity != null) {
            //unlocked
            Long id = to.getId();
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();

            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                if (data == null || data.getStatus() == 4) {
                    //??????????????????,???????????????
                    if(detailEntity.getLockStatus()==1){
                        //??????????????????????????????1??????????????????????????????????????????
                        unlockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //??????????????????????????????????????????????????????
                throw new RuntimeException("??????????????????");
            }
        }

    }

    //????????????????????????????????????????????????????????????
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        R r = orderFeignService.getOrderStatus(orderSn);
        WareOrderTaskEntity taskEntity = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = taskEntity.getId();
        //????????????????????????????????????????????????????????????
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id).eq("lock_status",1));

        for (WareOrderTaskDetailEntity entity : entities) {
            unlockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareIds;
    }

}