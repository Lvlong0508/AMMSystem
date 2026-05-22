package com.gzasc.aishopping.auth.controller.internal;

import com.gzasc.aishopping.auth.dto.RegisterEmployeeRequest;
import com.gzasc.aishopping.auth.service.MerchantAuthService;
import com.gzasc.aishopping.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalController {

    private final MerchantAuthService merchantAuthService;

    @PostMapping("/register-employee")
    public ApiResponse<Map<String, Object>> registerEmployee(@RequestBody @Valid RegisterEmployeeRequest request) {
        Long merchantId = merchantAuthService.registerEmployee(request);
        return ApiResponse.success("店员账号注册成功", Map.of("merchantId", merchantId));
    }
}