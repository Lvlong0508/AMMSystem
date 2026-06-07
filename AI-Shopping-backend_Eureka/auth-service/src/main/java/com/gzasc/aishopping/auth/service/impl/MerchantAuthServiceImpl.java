package com.gzasc.aishopping.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.mapper.merchant.MerchantMapper;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterEmployeeRequest;
import com.gzasc.aishopping.auth.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.MerchantAuthService;
import com.gzasc.aishopping.auth.service.MerchantInfoService;
import com.gzasc.aishopping.auth.util.BCryptUtil;
import com.gzasc.aishopping.common.util.SafeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MerchantAuthServiceImpl implements MerchantAuthService {

    private final MerchantMapper merchantMapper;
    private final MerchantInfoService merchantInfoService;

    // 商家用户信息服务

    @Override
    @Transactional
    public LoginResult register(RegisterRequest request) {
        // 1. 校验用户名是否已存在
        if (this.existsByUsername(request.getUsername())) {
            throw new AuthException("商家用户名已存在");
        }

        // 2. 验证手机号是否已被注册
        if (StringUtils.hasText(request.getPhone()) &&
                this.existsByPhone(request.getPhone())) {
            throw new AuthException("手机号已被注册");
        }

        // 3. 创建商家个人信息
        Integer infoId = null;
        if (StringUtils.hasText(request.getNickname())) {
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setNickname(request.getNickname());
            infoId = merchantInfoService.createMerchantInfo(merchantInfo);
        }

        // 4. 创建商家账号
        Merchant merchant = new Merchant();
        merchant.setId(SafeIdGenerator.nextId());
        merchant.setUsername(request.getUsername());
        merchant.setPassword(BCryptUtil.hashPassword(request.getPassword()));
        merchant.setPhone(request.getPhone());
        merchant.setEmail(request.getEmail());
        merchant.setInfoId(infoId);
        merchant.setStatus(1);

        merchantMapper.insert(merchant);

        StpUtil.login(merchant.getId());
        StpUtil.getTokenSession().set("accountType", "MERCHANT");
        String token = StpUtil.getTokenValue();

        return new LoginResult(token, merchant, "MERCHANT");
    }

    @Override
    public LoginResult login(String username, String password) {
        // 1. 根据账户名查询商家账号
        Merchant merchant = merchantMapper.selectByUsername(username);
        if (merchant == null) {
            throw new AuthException("用户名或密码错误");
        }

        // 2. 验证密码
        boolean passwordMatch = BCryptUtil.verifyPassword(password, merchant.getPassword());
        if (!passwordMatch) {
            throw new AuthException("用户名或密码错误");
        }

        // 3. 验证账号状态
        if (merchant.getStatus() == 0) {
            throw new AuthException("账号已被禁用");
        }

        StpUtil.login(merchant.getId());
        StpUtil.getTokenSession().set("accountType", "MERCHANT");
        String token = StpUtil.getTokenValue();

        return new LoginResult(token, merchant, "MERCHANT");
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    // 给店主用来创建店员账号的方法，网关验证店主身份后调用
    @Override
    @Transactional
    public Long registerEmployee(RegisterEmployeeRequest request) {
        // 1. 校验用户名是否已存在
        if (merchantMapper.countByUsername(request.getUsername()) > 0) {
            throw new AuthException("用户名已存在");
        }

        // 2. 验证手机号是否已被注册
        if (StringUtils.hasText(request.getPhone()) &&
                merchantMapper.countByPhone(request.getPhone()) > 0) {
            throw new AuthException("手机号已被注册");
        }

        // 3. 创建店员个人信息
        Integer infoId = null;
        if (StringUtils.hasText(request.getNickname())) {
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setNickname(request.getNickname());
            infoId = merchantInfoService.createMerchantInfo(merchantInfo);
        }

        // 4. 创建店员账号
        Merchant employee = new Merchant();
        employee.setId(SafeIdGenerator.nextId());
        employee.setUsername(request.getUsername());
        employee.setPhone(request.getPhone());
        employee.setStatus(1);
        employee.setInfoId(infoId);

        String password = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "123456";
        employee.setPassword(BCryptUtil.hashPassword(password));

        merchantMapper.insert(employee);

        return employee.getId();
    }

    // 辅助方法：检查用户名是否已存在
    @Override
    public boolean existsByUsername(String username) {
        return merchantMapper.countByUsername(username) > 0;
    }

    // 辅助方法：检查手机号是否已被注册
    @Override
    public boolean existsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return merchantMapper.countByPhone(phone) > 0;
    }

    // 辅助方法：根据商家ID查询商家账号信息
    @Override
    public Merchant getMerchantById(Long id) {
        return merchantMapper.selectById(id);
    }
}