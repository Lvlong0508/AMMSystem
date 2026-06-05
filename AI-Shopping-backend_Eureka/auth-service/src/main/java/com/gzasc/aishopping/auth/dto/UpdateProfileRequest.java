package com.gzasc.aishopping.auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;

    private String avatar;

    private String phone;

    private String email;
}
