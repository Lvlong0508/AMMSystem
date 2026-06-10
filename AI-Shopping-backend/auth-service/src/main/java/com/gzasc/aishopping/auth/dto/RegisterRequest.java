package com.gzasc.aishopping.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为3-20位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为6-20位")
    private String password;

    private String nickname;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;

    /** 店铺信息（可选，商家注册时一并创建店铺） */
    @Valid
    private ShopInfo shop;

    @Data
    public static class ShopInfo {
        @NotBlank(message = "店铺名称不能为空")
        @Size(max = 100, message = "店铺名称最长100个字符")
        private String name;

        @Size(max = 500, message = "店铺描述最长500个字符")
        private String description;

        private String logoUrl;
    }
}