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
    public UserInfo createUserInfo(String nickname) {
        UserInfo userInfo = new UserInfo();
        userInfo.setNickname(nickname);
        userInfoMapper.insert(userInfo);
        return userInfo;
    }
}