package com.gzasc.aishopping.auth.service;

import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterEmployeeRequest;
import com.gzasc.aishopping.auth.dto.RegisterRequest;

public interface MerchantAuthService {

    LoginResult register(RegisterRequest request);

    LoginResult login(String username, String password);

    Long registerEmployee(RegisterEmployeeRequest request);

    void logout();

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    Merchant getMerchantById(Long id);
}