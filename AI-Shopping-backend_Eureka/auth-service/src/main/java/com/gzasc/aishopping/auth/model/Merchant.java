package com.gzasc.aishopping.auth.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * 商家用户实体类
 * 密码使用 BCrypt 加盐加密存储，格式: $2a$12$随机盐值+密文
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {
    private Integer id;           // 商家ID
    private String username;      // 商家用户名
    private String password;      // BCrypt加密密码（含Salt）
    private String shopName;      // 店铺名称
    private String phone;         // 联系电话
    private String email;         // 邮箱
    private Integer status;       // 状态：0禁用 1启用
    private Date createdAt;       // 创建时间
    private Date updatedAt;       // 更新时间
}
