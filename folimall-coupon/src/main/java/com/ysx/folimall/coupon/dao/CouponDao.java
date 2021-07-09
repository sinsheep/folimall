package com.ysx.folimall.coupon.dao;

import com.ysx.folimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 21:08:44
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
