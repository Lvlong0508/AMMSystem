package com.gzasc.aishopping.auth.service;

import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.dto.LoginResult;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;

/**
 * 认证服务接口
 */
public interface AuthService {

    // ==================== 用户接口 ====================

    /**
     * 用户注册
     * @param request 注册请求
     * @return 登录结果（包含 Token）
     */
    LoginResult userRegister(RegisterRequest request);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录结果（包含 Token）
     */
    LoginResult userLogin(String username, String password);

    /**
     * 根据ID查询用户
     */
    User getUserById(Integer id);

    /**
     * 检查用户名是否存在（用户）
     */
    boolean userExistsByUsername(String username);

    /**
     * 检查手机号是否存在（用户）
     */
    boolean userExistsByPhone(String phone);

    // ==================== 商家接口 ====================

    /**
     * 商家注册
     * @param request 注册请求
     * @return 登录结果（包含 Token）
     */
    LoginResult merchantRegister(RegisterRequest request);

    /**
     * 商家登录
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录结果（包含 Token）
     */
    LoginResult merchantLogin(String username, String password);

    /**
     * 根据ID查询商家
     */
    Merchant getMerchantById(Integer id);

    /**
     * 检查用户名是否存在（商家）
     */
    boolean merchantExistsByUsername(String username);

    /**
     * 检查手机号是否存在（商家）
     */
    boolean merchantExistsByPhone(String phone);

    // ==================== 通用接口 ====================

    /**
     * 登出当前登录账号
     */
    void logout();
}
