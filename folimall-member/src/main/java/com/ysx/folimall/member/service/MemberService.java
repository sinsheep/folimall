package com.ysx.folimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ysx.common.utils.PageUtils;
import com.ysx.folimall.member.vo.MemberLoginVo;
import com.ysx.folimall.member.vo.MemberRegistVo;
import com.ysx.folimall.member.entity.MemberEntity;
import com.ysx.folimall.member.exception.PhoneExistException;
import com.ysx.folimall.member.exception.UsernameExistException;

import java.util.Map;

/**
 * 会员
 *
 * @author ysx
 * @email sheepsx@qq.com
 * @date 2021-07-05 21:18:41
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;
    void checkUsernameUnique(String username) throws UsernameExistException;

    MemberEntity login(MemberLoginVo vo);
}

