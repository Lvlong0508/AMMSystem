package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.model.UserInfo;
import com.gzasc.aishopping.auth.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/userinfo")
@RequiredArgsConstructor
public class InternalUserInfoController {

    private final UserInfoService userInfoService;

    @PostMapping("/create")
    public Map<String, Object> createUserInfo(@RequestBody Map<String, String> request) {
        String nickname = request.get("nickname");
        try {
            UserInfo userInfo = userInfoService.createUserInfo(nickname);
            return Map.of(
                "success", true,
                "infoId", userInfo.getId()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "创建用户信息失败：" + e.getMessage()
            );
        }
    }
}