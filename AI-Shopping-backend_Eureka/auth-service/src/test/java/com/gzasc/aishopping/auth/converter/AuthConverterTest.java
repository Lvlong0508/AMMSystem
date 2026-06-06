package com.gzasc.aishopping.auth.converter;

import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthConverterTest {

    private final AuthConverter converter = new AuthConverter();

    @Test
    @DisplayName("toUserInfoMap 应转换 User 和 UserInfo 到 Map")
    void toUserInfoMap_withUserInfo() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setPhone("13800138001");
        user.setEmail("test@test.com");
        user.setInfoId(1);
        user.setStatus(1);

        UserInfo userInfo = new UserInfo();
        userInfo.setNickname("测试昵称");
        userInfo.setAvatar("avatar.jpg");

        Map<String, Object> result = converter.toUserInfoMap(user, userInfo);

        assertEquals("100", result.get("id"));
        assertEquals("testuser", result.get("username"));
        assertEquals("13800138001", result.get("phone"));
        assertEquals("test@test.com", result.get("email"));
        assertEquals(1, result.get("infoId"));
        assertEquals(1, result.get("status"));
        assertEquals("测试昵称", result.get("nickname"));
        assertEquals("avatar.jpg", result.get("avatar"));
    }

    @Test
    @DisplayName("toUserInfoMap 当 UserInfo 为 null 时应忽略昵称和头像")
    void toUserInfoMap_withoutUserInfo() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");

        Map<String, Object> result = converter.toUserInfoMap(user, null);

        assertEquals("100", result.get("id"));
        assertEquals("testuser", result.get("username"));
        assertNull(result.get("nickname"));
        assertNull(result.get("avatar"));
    }

    @Test
    @DisplayName("toMerchantInfoMap 应转换 Merchant 和 MerchantInfo 到 Map")
    void toMerchantInfoMap_withMerchantInfo() {
        Merchant merchant = new Merchant();
        merchant.setId(200L);
        merchant.setUsername("seller1");
        merchant.setPhone("13900139001");
        merchant.setEmail("seller@shop.com");
        merchant.setInfoId(2);
        merchant.setStatus(1);

        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setNickname("商家一号");
        merchantInfo.setAvatar("shop.jpg");

        Map<String, Object> result = converter.toMerchantInfoMap(merchant, merchantInfo);

        assertEquals("200", result.get("id"));
        assertEquals("seller1", result.get("username"));
        assertEquals("13900139001", result.get("phone"));
        assertEquals("seller@shop.com", result.get("email"));
        assertEquals(2, result.get("infoId"));
        assertEquals(1, result.get("status"));
        assertEquals("商家一号", result.get("nickname"));
        assertEquals("shop.jpg", result.get("avatar"));
    }

    @Test
    @DisplayName("toMerchantInfoMap 当 MerchantInfo 为 null 时应忽略昵称和头像")
    void toMerchantInfoMap_withoutMerchantInfo() {
        Merchant merchant = new Merchant();
        merchant.setId(200L);
        merchant.setUsername("seller1");

        Map<String, Object> result = converter.toMerchantInfoMap(merchant, null);

        assertEquals("200", result.get("id"));
        assertEquals("seller1", result.get("username"));
        assertNull(result.get("nickname"));
        assertNull(result.get("avatar"));
    }
}
