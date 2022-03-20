package com.ysx.folimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ysx.common.constant.OrderConstant;
import com.ysx.common.enume.OrderStatusEnum;
import com.ysx.common.exception.NoStockException;
import com.ysx.common.to.MemberRespTo;
import com.ysx.common.to.OrderTo;
import com.ysx.common.utils.R;
import com.ysx.folimall.order.entity.OrderItemEntity;
import com.ysx.folimall.order.feign.CartFeignService;
import com.ysx.folimall.order.feign.MemberFeignService;
import com.ysx.folimall.order.feign.ProductFeignService;
import com.ysx.folimall.order.feign.WmsFeignService;
import com.ysx.folimall.order.interceptor.LoginUserInterceptor;
import com.ysx.folimall.order.service.OrderItemService;
import com.ysx.folimall.order.to.OrderCreateTo;
import com.ysx.folimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.order.dao.OrderDao;
import com.ysx.folimall.order.entity.OrderEntity;
import com.ysx.folimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {


    private  ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    WmsFeignService wmsFeignService;
    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //远程查询收获地址列表
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespTo.getId());
            confirmVo.setAddress(address);
        }, executor);
        //远程查询购物车中的购物项目
        CompletableFuture<Void> getCartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R skusHasStock = wmsFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = skusHasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            Map<Long, Boolean> stocks = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
            confirmVo.setStocks(stocks);
        });
        //查询用户积分
        Integer integration = memberRespTo.getIntegration();
        confirmVo.setIntegration(integration);
        CompletableFuture.allOf(getAddressFuture,getCartFuture).get();
        String token = UUID.randomUUID().toString().replace("_", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberRespTo.getId(),token,30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);
        return confirmVo;
    }

    @Override
    @Transactional
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();
        String orderToken = vo.getOrderToken();
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script,Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberRespTo.getId()),orderToken);
        if(result == 0L){
            responseVo.setCode(1);
            return responseVo;
        } else {
            OrderCreateTo order = createOrder();
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue())<0.01){
                //金额对比成功
                //保存订单
                saveOrder(order);
                //订单号，订单项(skuId,skuName,num);
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn())
;
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //远程锁库存
                R r = wmsFeignService.orderLockStock(lockVo);
                if(r.getCode()==0){
                    responseVo.setOrderEntity(order.getOrder());
                    // TODO: 10/16/21 远程扣减积分
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());
                    return responseVo;
                }else{
                    //锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException();
                }
            }else{
                responseVo.setCode(2);
            }
            return responseVo;
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        OrderEntity orderEntity = this.getById(entity.getId());
        if(orderEntity.getStatus()==OrderStatusEnum.CREATE_NEW.getCode()){
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCELED.getCode());
            this.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity,orderTo);
            //发给mq一个信息
            rabbitTemplate.convertAndSend("order-event-exchange"
            ,"order.release.other",orderTo);
        }
    }

    private void saveOrder(OrderCreateTo createTO) {
        OrderEntity orderEntity = createTO.getOrder();
        orderEntity.setModifyTime(new Date());
        baseMapper.insert(orderEntity);
        List<OrderItemEntity> orderItems = createTO.getOrderItems();

        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder(){
        OrderCreateTo createTO = new OrderCreateTo();
        //1。 生成订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单号
        OrderEntity entity = buildOrder(orderSn);

        //获取订单项数据
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        //验价格,积分等信息
        computePrice(entity,itemEntities);
        createTO.setOrder(entity);
        createTO.setOrderItems(itemEntities);
        return createTO;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0");

        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        for (OrderItemEntity entity : itemEntities) {
            BigDecimal realAmount = entity.getRealAmount();
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(realAmount);
            gift = gift.add(BigDecimal.valueOf(entity.getGiftGrowth()));
            growth = growth.add(BigDecimal.valueOf(entity.getGiftGrowth()));
        }
        //1订单价格相关
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);
        //设置积分等信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setDeleteStatus(0);//伟删除
    }

    private OrderEntity buildOrder(String orderSn) {
        MemberRespTo respTo = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(respTo.getId());
        //获取收获地址
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        entity.setFreightAmount(fareResp.getFare());
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        //设置订单状态
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        return entity;
    }

    /**
     * 构建订单项
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems!=null && currentUserCartItems.size()>0){
            List<OrderItemEntity> collect = currentUserCartItems.stream().map(cartIem -> {
                OrderItemEntity itemEntity = builderOrderItem(cartIem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 构建某一个订单项
     * @param cartIem
     * @return
     */
    private OrderItemEntity builderOrderItem(OrderItemVo cartIem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        //订单信息，订单号
        //2.订单spu信息
        Long skuId = cartIem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());
        //商品sku信息
        itemEntity.setSkuId(cartIem.getSkuId());
        itemEntity.setSkuName(cartIem.getTitle());
        itemEntity.setSkuPic(cartIem.getImage());
        itemEntity.setSkuPrice(cartIem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartIem.getSkuAttr(),";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartIem.getCount());


        //、、优惠信息
        //积分信息
        itemEntity.setGiftGrowth(cartIem.getPrice().multiply(BigDecimal.valueOf(cartIem.getCount())).intValue());
        itemEntity.setGiftIntegration(cartIem.getPrice().multiply(BigDecimal.valueOf(cartIem.getCount())).intValue());

        //订单项价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //订单实际金额
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal realAmount = origin.subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(realAmount);
        return itemEntity;
    }
}