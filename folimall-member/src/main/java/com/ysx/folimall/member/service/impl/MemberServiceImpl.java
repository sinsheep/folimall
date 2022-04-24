package com.ysx.folimall.member.service.impl;

import com.ysx.folimall.member.vo.MemberLoginVo;
import com.ysx.folimall.member.vo.MemberRegistVo;
import com.ysx.folimall.member.dao.MemberLevelDao;
import com.ysx.folimall.member.entity.MemberLevelEntity;
import com.ysx.folimall.member.exception.PhoneExistException;
import com.ysx.folimall.member.exception.UsernameExistException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ysx.common.utils.PageUtils;
import com.ysx.common.utils.Query;

import com.ysx.folimall.member.dao.MemberDao;
import com.ysx.folimall.member.entity.MemberEntity;
import com.ysx.folimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity member = new MemberEntity();
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        member.setLevelId(levelEntity.getId());

        //检验手机和帐号唯一性
        checkPhoneUnique(vo.getPhone());
        member.setMobile(vo.getPhone());
        checkUsernameUnique(vo.getUserName());
        member.setUsername(vo.getUserName());
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        member.setPassword(bCryptPasswordEncoder.encode(vo.getPassword()));
        member.setNickname(vo.getUserName());;
        baseMapper.insert(member);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException{
        Integer mobile = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(mobile>0){
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException{
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(count>0){
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct)
                .or().eq("mobile", loginacct));
        if(entity == null){
            return null;
        }else{
            String passwordDb = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if(matches){
                System.out.println("---"+entity);
                return entity;
            }else{
                // TODO: 4/10/22 need to fix 
                return entity;
            }
        }
    }

}