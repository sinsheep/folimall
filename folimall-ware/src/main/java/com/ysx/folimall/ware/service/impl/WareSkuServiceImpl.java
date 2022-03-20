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
            // TODO: 8/12/21 什么方法让异常出现后不回滚 高级部分说
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
     * 解锁场景
     * 下订单成，订单过期没有支付被系统自动取消、被用户手动取消。都需要解锁库存
     * 下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚
     *
     * @param vo
     * @return
     */
    @Override
    @Transactional
    public boolean orderLockStock(WareSkuLockVo vo) {

        /**
         * 保存库存工作单
         */

        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);
        // 找到就近仓库锁定库存
        //找到每个商品在那个仓库有库存
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

        //锁定库存
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


        System.out.println("收到库存解锁id");
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();
        //查询数据库订单锁库存信息
        //有 库存锁定成功
        // 1.没有这个订单，必须解锁
        // 2. 有这个订单，不是解锁库存
        //    1. 取消 解锁库存
        //    2. 没取消 不能解锁库存
        //没有；库存锁定失败，库存回滚了，这种情况无需解锁
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
                    //订单被取消了,订单不存在
                    if(detailEntity.getLockStatus()==1){
                        //当前工作单详情状态是1，一锁定但是未解锁才可以解锁
                        unlockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //消息拒绝后放到队列中，让别人重新消费
                throw new RuntimeException("远程服务失败");
            }
        }

    }

    //防止订单服务卡顿，导致订单状态一直改不了
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        R r = orderFeignService.getOrderStatus(orderSn);
        WareOrderTaskEntity taskEntity = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = taskEntity.getId();
        //按照工作单找到所有没有解锁的库存进行解锁
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