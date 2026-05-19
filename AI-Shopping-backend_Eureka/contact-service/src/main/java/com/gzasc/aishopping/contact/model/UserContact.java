package com.gzasc.aishopping.contact.model;

import lombok.Data;

/**
 * 用户-联系人关联实体类
 * 对应数据库 user_contact 表，建立用户与联系人的多对多关系
 */
@Data
public class UserContact {
    private Integer id;        // 记录ID
    private Integer userId;     // 用户ID
    private Integer contactId; // 联系人ID
}