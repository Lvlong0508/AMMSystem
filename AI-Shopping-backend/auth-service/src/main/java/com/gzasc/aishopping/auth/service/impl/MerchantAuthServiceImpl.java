package com.gzasc.aishopping.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.mapper.merchant.MerchantMapper;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.MerchantAuthService;
import com.gzasc.aishopping.auth.service.MerchantInfoService;
import com.gzasc.aishopping.auth.util.BCryptUtil;
import com.gzasc.aishopping.common.dto.shop.CreateShopForMerchantRequest;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.common.util.SafeIdGenerator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAuthServiceImpl implements MerchantAuthService {

    private final MerchantMapper merchantMapper;
    private final MerchantInfoService merchantInfoService;
    private final ShopFeignClient shopFeignClient;

    @Override
    @Transactional
    public LoginResult register(RegisterRequest request) {
        if (this.existsByUsername(request.getUsername())) {
            throw new AuthException("商家用户名已存在");
        }
        if (StringUtils.hasText(request.getPhone()) &&
                this.existsByPhone(request.getPhone())) {
            throw new AuthException("手机号已被注册");
        }

        Integer infoId = null;
        if (StringUtils.hasText(request.getNickname())) {
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setNickname(request.getNickname());
            infoId = merchantInfoService.createMerchantInfo(merchantInfo);
        }

        Merchant merchant = new Merchant();
        merchant.setId(SafeIdGenerator.nextId());
        merchant.setUsername(request.getUsername());
        merchant.setPassword(BCryptUtil.hashPassword(request.getPassword()));
        merchant.setPhone(request.getPhone());
        merchant.setEmail(request.getEmail());
        merchant.setInfoId(infoId);
        merchant.setStatus(1);

        merchantMapper.insert(merchant);

        if (request.getShop() != null) {
            CreateShopForMerchantRequest shopReq = new CreateShopForMerchantRequest();
            shopReq.setMerchantId(merchant.getId());
            shopReq.setName(request.getShop().getName());
            shopReq.setDescription(request.getShop().getDescription());
            shopReq.setLogoUrl(request.getShop().getLogoUrl());

            try {
                ApiResponse<Map<String, Object>> shopResponse = shopFeignClient.createShopForMerchant("auth-service", shopReq);
                if (shopResponse == null || shopResponse.getCode() != 200) {
                    throw new AuthException("创建店铺失败: " + (shopResponse == null ? "无响应" : shopResponse.getMessage()));
                }
                log.info("商家注册同时创建店铺成功, merchantId={}", merchant.getId());
            } catch (Exception e) {
                log.error("调用shop-service创建店铺失败, merchantId={}", merchant.getId(), e);
                throw new AuthException("创建店铺失败，注册已回滚");
            }
        }

        StpUtil.login(merchant.getId());
        StpUtil.getTokenSession().set("accountType", "MERCHANT");
        String token = StpUtil.getTokenValue();

        return new LoginResult(token, merchant, "MERCHANT");
    }

    @Override
    public LoginResult login(String username, String password) {
        Merchant merchant = merchantMapper.selectByUsername(username);
        if (merchant == null) {
            throw new AuthException("用户名或密码错误");
        }
        boolean passwordMatch = BCryptUtil.verifyPassword(password, merchant.getPassword());
        if (!passwordMatch) {
            throw new AuthException("用户名或密码错误");
        }
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

    @Override
    public boolean existsByUsername(String username) {
        return merchantMapper.countByUsername(username) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return merchantMapper.countByPhone(phone) > 0;
    }

    @Override
    public Merchant getMerchantById(Long id) {
        return merchantMapper.selectById(id);
    }
}