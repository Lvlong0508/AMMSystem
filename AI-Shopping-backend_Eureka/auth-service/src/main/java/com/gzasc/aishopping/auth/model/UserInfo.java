package com.gzasc.aishopping.auth.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * 用户基础信息实体类（通用：用户和商家共用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private Integer id;           // 信息ID
    private String nickname;      // 昵称
    private String avatar;         // 头像URL
    private Date createdAt;       // 创建时间
    private Date updatedAt;       // 更新时间
}