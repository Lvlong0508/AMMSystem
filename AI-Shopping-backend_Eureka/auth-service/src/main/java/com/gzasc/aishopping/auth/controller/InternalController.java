package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.model.dto.RegisterEmployeeRequest;
import com.gzasc.aishopping.auth.service.AuthService;
import com.gzasc.aishopping.auth.service.impl.AuthException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalController {

    private final AuthService authService;

    @PostMapping("/register-employee")
    public Map<String, Object> registerEmployee(@RequestBody @Valid RegisterEmployeeRequest request,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            Integer merchantId = authService.registerEmployee(request);
            return Map.of(
                "message", "店员账号注册成功",
                "merchantId", merchantId
            );
        } catch (AuthException e) {
            return Map.of("message", "注册失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("message", "注册错误：" + e.getMessage());
        }
    }
}