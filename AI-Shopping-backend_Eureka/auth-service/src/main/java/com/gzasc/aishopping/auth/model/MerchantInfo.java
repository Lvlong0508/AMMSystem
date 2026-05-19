package com.gzasc.aishopping.auth.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInfo {
    private Integer id;
    private String nickname;
    private String avatar;
    private Date createdAt;
    private Date updatedAt;
}