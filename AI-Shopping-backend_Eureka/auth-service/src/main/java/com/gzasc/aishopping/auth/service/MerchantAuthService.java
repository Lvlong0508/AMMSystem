package com.gzasc.aishopping.auth.service;

import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.dto.LoginResult;
import com.gzasc.aishopping.auth.model.dto.RegisterEmployeeRequest;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;

public interface MerchantAuthService {

    LoginResult register(RegisterRequest request);

    LoginResult login(String username, String password);

    Integer registerEmployee(RegisterEmployeeRequest request);

    void logout();

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    Merchant getMerchantById(Integer id);
}