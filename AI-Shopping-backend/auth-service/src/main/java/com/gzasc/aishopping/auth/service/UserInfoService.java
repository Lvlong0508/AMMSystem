package com.gzasc.aishopping.auth.service;

import com.gzasc.aishopping.auth.model.UserInfo;

public interface UserInfoService {

    UserInfo getUserInfoById(Integer id);

    Integer createUserInfo(UserInfo info);

    void updateUserInfo(UserInfo info);
}