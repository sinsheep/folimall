package com.ysx.folimall.member.dao;

import com.ysx.folimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 21:18:41
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
