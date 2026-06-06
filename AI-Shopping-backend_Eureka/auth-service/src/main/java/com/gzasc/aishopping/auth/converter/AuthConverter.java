package com.gzasc.aishopping.auth.converter;

import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.UserInfo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthConverter {

    public Map<String, Object> toUserInfoMap(User user, UserInfo userInfo) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", String.valueOf(user.getId()));
        info.put("username", user.getUsername());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("infoId", user.getInfoId());
        info.put("status", user.getStatus());
        if (userInfo != null) {
            info.put("nickname", userInfo.getNickname());
            info.put("avatar", userInfo.getAvatar());
        }
        return info;
    }

    public Map<String, Object> toMerchantInfoMap(Merchant merchant, MerchantInfo merchantInfo) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", String.valueOf(merchant.getId()));
        info.put("username", merchant.getUsername());
        info.put("phone", merchant.getPhone());
        info.put("email", merchant.getEmail());
        info.put("infoId", merchant.getInfoId());
        info.put("status", merchant.getStatus());
        if (merchantInfo != null) {
            info.put("nickname", merchantInfo.getNickname());
            info.put("avatar", merchantInfo.getAvatar());
        }
        return info;
    }
}
