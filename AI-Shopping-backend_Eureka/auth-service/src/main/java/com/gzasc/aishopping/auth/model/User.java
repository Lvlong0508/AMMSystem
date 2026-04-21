package com.gzasc.aishopping.auth.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * 消费者用户实体类
 * 密码使用 BCrypt 加盐加密存储，格式: $2a$12$随机盐值+密文
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;           // 用户ID
    private String username;      // 用户名
    private String password;      // BCrypt加密密码（含Salt）
    private String phone;         // 手机号
    private String email;         // 邮箱
    private String nickname;      // 昵称
    private Integer status;       // 状态：0禁用 1启用
    private Date createdAt;       // 创建时间
    private Date updatedAt;       // 更新时间
}
