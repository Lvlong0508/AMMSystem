package com.gzasc.aishopping.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.auth.mapper.merchant.MerchantMapper;
import com.gzasc.aishopping.auth.mapper.user.UserMapper;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.dto.LoginResult;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;
import com.gzasc.aishopping.auth.model.dto.RegisterEmployeeRequest;
import com.gzasc.aishopping.auth.service.AuthService;
import com.gzasc.aishopping.auth.util.BCryptUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 认证服务实现类
 * 
 * 核心功能：
 * 1. 使用 BCrypt 进行加盐加密（自动生成随机 Salt）
 * 2. 使用 Sa-Token 进行登录认证和 Token 管理
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;

    // ==================== 用户接口实现 ====================

    @Override
    @Transactional
    public LoginResult userRegister(RegisterRequest request) {
        System.out.println(new Date() + ": run userRegister, username=" + request.getUsername());

        // 检查用户名唯一性
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new AuthException("用户名已存在");
        }

        // 检查手机号唯一性（如果提供）
        if (StringUtils.hasText(request.getPhone()) &&
                userMapper.countByPhone(request.getPhone()) > 0) {
            throw new AuthException("手机号已被注册");
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());

        // ========== 核心：BCrypt 加盐加密存储 ==========
        // BCrypt.hashpw 自动生成随机 Salt，并将 Salt 嵌入密文中
        // 结果格式: $2a$12$22位随机盐值+31位密文
        String encryptedPassword = BCryptUtil.hashPassword(request.getPassword());
        user.setPassword(encryptedPassword);
        // =============================================

        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(1);

        userMapper.insert(user);
        System.out.println(new Date() + ": user created, id=" + user.getId());

        // 使用 Sa-Token 登录（自动创建 Token 并存入 Redis）
        StpUtil.login("USER:" + user.getId());
        String token = StpUtil.getTokenValue();
        System.out.println(new Date() + ": user login success, token=" + token.substring(0, 20) + "...");

        return new LoginResult(token, user);
    }

    @Override
    public LoginResult userLogin(String username, String password) {
        System.out.println(new Date() + ": run userLogin, username=" + username);

        // 查询用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            System.out.println(new Date() + ": user not found");
            throw new AuthException("用户名或密码错误");
        }

        // ========== 核心：BCrypt 加盐验证密码 ==========
        // BCrypt.checkpw 自动从 storedHash 中提取 Salt，然后进行验证
        boolean passwordMatch = BCryptUtil.verifyPassword(password, user.getPassword());
        if (!passwordMatch) {
            System.out.println(new Date() + ": password not match");
            throw new AuthException("用户名或密码错误");
        }
        // =============================================

        if (user.getStatus() == 0) {
            throw new AuthException("账号已被禁用");
        }

        System.out.println(new Date() + ": password verified, login success");

        // 使用 Sa-Token 登录
        StpUtil.login("USER:" + user.getId());
        String token = StpUtil.getTokenValue();

        return new LoginResult(token, user);
    }

    @Override
    public User getUserById(Integer id) {
        return userMapper.selectById(id);
    }

    @Override
    public boolean userExistsByUsername(String username) {
        return userMapper.countByUsername(username) > 0;
    }

    @Override
    public boolean userExistsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return userMapper.countByPhone(phone) > 0;
    }

    // ==================== 商家接口实现 ====================

    @Override
    @Transactional
    public LoginResult merchantRegister(RegisterRequest request) {
        System.out.println(new Date() + ": run merchantRegister, username=" + request.getUsername());

        // 检查用户名唯一性
        if (merchantMapper.countByUsername(request.getUsername()) > 0) {
            throw new AuthException("商家用户名已存在");
        }

        // 检查手机号唯一性
        if (StringUtils.hasText(request.getPhone()) &&
                merchantMapper.countByPhone(request.getPhone()) > 0) {
            throw new AuthException("手机号已被注册");
        }

        // 创建商家实体
        Merchant merchant = new Merchant();
        merchant.setUsername(request.getUsername());

        // ========== 核心：BCrypt 加盐加密存储 ==========
        String encryptedPassword = BCryptUtil.hashPassword(request.getPassword());
        merchant.setPassword(encryptedPassword);
        // =============================================

        merchant.setShopName(request.getNickname()); // 昵称作为店铺名
        merchant.setPhone(request.getPhone());
        merchant.setStatus(1);

        merchantMapper.insert(merchant);
        System.out.println(new Date() + ": merchant created, id=" + merchant.getId());

        // 使用 Sa-Token 登录（前缀 MERCHANT 区分用户类型）
        StpUtil.login("MERCHANT:" + merchant.getId());
        String token = StpUtil.getTokenValue();

        return new LoginResult(token, merchant);
    }

    @Override
    public LoginResult merchantLogin(String username, String password) {
        System.out.println(new Date() + ": run merchantLogin, username=" + username);

        // 查询商家
        Merchant merchant = merchantMapper.selectByUsername(username);
        if (merchant == null) {
            throw new AuthException("用户名或密码错误");
        }

        // ========== 核心：BCrypt 加盐验证密码 ==========
        boolean passwordMatch = BCryptUtil.verifyPassword(password, merchant.getPassword());
        if (!passwordMatch) {
            throw new AuthException("用户名或密码错误");
        }
        // =============================================

        if (merchant.getStatus() == 0) {
            throw new AuthException("账号已被禁用");
        }

        // 使用 Sa-Token 登录
        StpUtil.login("MERCHANT:" + merchant.getId());
        String token = StpUtil.getTokenValue();

        return new LoginResult(token, merchant);
    }

    @Override
    public Merchant getMerchantById(Integer id) {
        return merchantMapper.selectById(id);
    }

    @Override
    public boolean merchantExistsByUsername(String username) {
        return merchantMapper.countByUsername(username) > 0;
    }

    @Override
    public boolean merchantExistsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return merchantMapper.countByPhone(phone) > 0;
    }

    // ==================== 店员接口实现 ====================

    @Override
    @Transactional
    public Integer registerEmployee(RegisterEmployeeRequest request) {
        System.out.println(new Date() + ": run registerEmployee, username=" + request.getUsername());

        if (merchantMapper.countByUsername(request.getUsername()) > 0) {
            throw new AuthException("用户名已存在");
        }

        if (StringUtils.hasText(request.getPhone()) &&
                merchantMapper.countByPhone(request.getPhone()) > 0) {
            throw new AuthException("手机号已被注册");
        }

        Merchant employee = new Merchant();
        employee.setUsername(request.getUsername());
        employee.setShopName(""); // 无店铺
        employee.setPhone(request.getPhone());
        employee.setStatus(1);

        String password = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "123456";
        employee.setPassword(BCryptUtil.hashPassword(password));

        merchantMapper.insert(employee);
        System.out.println(new Date() + ": employee created, id=" + employee.getId());

        return employee.getId();
    }

    // ==================== 通用接口实现 ====================

    @Override
    public void logout() {
        System.out.println(new Date() + ": run logout");
        StpUtil.logout();
    }
}
