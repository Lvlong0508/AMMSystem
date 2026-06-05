package com.gzasc.aishopping.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.mapper.user.UserMapper;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.UserInfo;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterRequest;
import com.gzasc.aishopping.auth.dto.UpdateProfileRequest;
import com.gzasc.aishopping.auth.service.UserAuthService;
import com.gzasc.aishopping.auth.service.UserInfoService;
import com.gzasc.aishopping.auth.util.BCryptUtil;
import com.gzasc.aishopping.common.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserMapper userMapper;
    private final UserInfoService userInfoService;

    @Override
    @Transactional
    public LoginResult register(RegisterRequest request) {
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new AuthException("用户名已存在");
        }

        if (StringUtils.hasText(request.getPhone()) &&
                userMapper.countByPhone(request.getPhone()) > 0) {
            throw new AuthException("手机号已被注册");
        }

        Integer infoId = null;
        if (StringUtils.hasText(request.getNickname())) {
            UserInfo userInfo = new UserInfo();
            userInfo.setNickname(request.getNickname());
            infoId = userInfoService.createUserInfo(userInfo);
        }

        User user = new User();
        user.setId(SnowflakeIdGenerator.nextId());
        user.setUsername(request.getUsername());
        user.setPassword(BCryptUtil.hashPassword(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setInfoId(infoId);
        user.setStatus(1);

        userMapper.insert(user);

        StpUtil.login(user.getId());
        StpUtil.getTokenSession().set("accountType", "USER");
        String token = StpUtil.getTokenValue();

        return new LoginResult(token, user, "USER");
    }

    @Override
    public LoginResult login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new AuthException("用户名或密码错误");
        }

        boolean passwordMatch = BCryptUtil.verifyPassword(password, user.getPassword());
        if (!passwordMatch) {
            throw new AuthException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new AuthException("账号已被禁用");
        }

        StpUtil.login(user.getId());
        StpUtil.getTokenSession().set("accountType", "USER");
        String token = StpUtil.getTokenValue();

        return new LoginResult(token, user, "USER");
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.countByUsername(username) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return userMapper.countByPhone(phone) > 0;
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }

        if (StringUtils.hasText(request.getNickname()) || StringUtils.hasText(request.getAvatar())) {
            UserInfo userInfo;
            if (user.getInfoId() != null) {
                userInfo = userInfoService.getUserInfoById(user.getInfoId());
                if (userInfo == null) {
                    userInfo = new UserInfo();
                    userInfo.setId(user.getInfoId());
                }
            } else {
                userInfo = new UserInfo();
            }
            if (StringUtils.hasText(request.getNickname())) {
                userInfo.setNickname(request.getNickname());
            }
            if (StringUtils.hasText(request.getAvatar())) {
                userInfo.setAvatar(request.getAvatar());
            }
            if (userInfo.getId() != null) {
                userInfoService.updateUserInfo(userInfo);
            } else {
                Integer infoId = userInfoService.createUserInfo(userInfo);
                user.setInfoId(infoId);
            }
        }

        if (StringUtils.hasText(request.getPhone())) {
            User existingByPhone = userMapper.selectByPhone(request.getPhone());
            if (existingByPhone != null && !existingByPhone.getId().equals(userId)) {
                throw new AuthException("手机号已被注册");
            }
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }

        userMapper.update(user);
    }
}