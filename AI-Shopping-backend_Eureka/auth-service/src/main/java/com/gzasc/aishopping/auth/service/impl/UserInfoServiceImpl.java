package com.gzasc.aishopping.auth.service.impl;

import com.gzasc.aishopping.auth.mapper.user.UserInfoMapper;
import com.gzasc.aishopping.auth.model.UserInfo;
import com.gzasc.aishopping.auth.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl implements UserInfoService {

    private final UserInfoMapper userInfoMapper;

    @Override
    public UserInfo getUserInfoById(Integer id) {
        return userInfoMapper.selectById(id);
    }

    @Override
    public Integer createUserInfo(UserInfo info) {
        userInfoMapper.insert(info);
        if (info.getId() == null) {
            throw new RuntimeException("创建用户信息失败");
        }
        return info.getId();
    }
}