package com.gzasc.aishopping.auth.service;

import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterRequest;

public interface UserAuthService {

    LoginResult register(RegisterRequest request);

    LoginResult login(String username, String password);

    void logout();

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    User getUserById(Long id);
}