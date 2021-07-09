package com.ysx.folimall.order.dao;

import com.ysx.folimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 21:30:18
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
