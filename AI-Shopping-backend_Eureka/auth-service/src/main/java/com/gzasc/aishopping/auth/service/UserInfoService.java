package com.gzasc.aishopping.auth.service;

import com.gzasc.aishopping.auth.model.UserInfo;

public interface UserInfoService {

    /**
     * 创建用户信息
     * @param nickname 昵称
     * @return 创建后的用户信息（含ID）
     */
    UserInfo createUserInfo(String nickname);
}