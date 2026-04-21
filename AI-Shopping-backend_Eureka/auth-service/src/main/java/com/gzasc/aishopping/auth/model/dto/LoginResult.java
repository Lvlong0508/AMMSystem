package com.gzasc.aishopping.auth.model.dto;

import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/注册结果 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {
    private String token;       // Sa-Token
    private User user;          // 用户信息（用户登录时）
    private Merchant merchant;  // 商家信息（商家登录时）
    
    public LoginResult(String token, User user) {
        this.token = token;
        this.user = user;
    }
    
    public LoginResult(String token, Merchant merchant) {
        this.token = token;
        this.merchant = merchant;
    }
}
