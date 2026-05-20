package com.gzasc.aishopping.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/注册结果 DTO
 */
@Data
@NoArgsConstructor
public class LoginResult<T> {
    private String token;
    private T account;
    private String accountType;

    public LoginResult(String token, T account, String accountType) {
        this.token = token;
        this.account = account;
        this.accountType = accountType;
    }
}
