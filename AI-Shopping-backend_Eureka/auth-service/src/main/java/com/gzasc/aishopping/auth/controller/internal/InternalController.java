package com.gzasc.aishopping.auth.controller.internal;

import com.gzasc.aishopping.auth.dto.RegisterEmployeeRequest;
import com.gzasc.aishopping.auth.service.MerchantAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalController {

    private final MerchantAuthService merchantAuthService;

    @PostMapping("/register-employee")
    public Map<String, Object> registerEmployee(@RequestBody @Valid RegisterEmployeeRequest request,
                                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("code", 400, "message", "参数错误：" + Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
        }

        Integer merchantId = merchantAuthService.registerEmployee(request);
        return Map.of(
                "code", 200,
                "message", "店员账号注册成功",
                "data", Map.of("merchantId", merchantId)
        );
    }
}