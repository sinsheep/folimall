package com.ysx.folimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.ysx.common.utils.R;
import com.ysx.folimall.ware.feign.MemberFeignService;
import com.ysx.folimall.ware.vo.FareVo;
import com.ysx.folimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.ware.dao.WareInfoDao;
import com.ysx.folimall.ware.entity.WareInfoEntity;
import com.ysx.folimall.ware.service.WareInfoService;
import org.springframework.util.ObjectUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!ObjectUtils.isEmpty(key)) {
            wrapper.eq("id", key)
                    .or().like("name", key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {

        FareVo fareVo = new FareVo();
        R r = memberFeignService.addrInfo(addrId);
        MemberAddressVo data = r.getData(new TypeReference<MemberAddressVo>() {
        });
        if(data!=null){
            String phone = data.getPhone();
            String substring = phone.substring(phone.length() - 2, phone.length() - 1);
            BigDecimal fare = new BigDecimal(substring);
            fareVo.setAddress(data);;
            fareVo.setFare(fare);
            return fareVo;
        }
        return null;
    }

}