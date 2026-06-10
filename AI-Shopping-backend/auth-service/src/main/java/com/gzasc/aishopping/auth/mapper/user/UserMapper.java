package com.gzasc.aishopping.auth.mapper.user;

import com.gzasc.aishopping.auth.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户 Mapper 接口
 * 使用 user 数据源（eureka_user 库）
 */
@Mapper
public interface UserMapper {

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User selectById(Long id);

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User selectByUsername(String username);

    /**
     * 查询所有用户
     */
    @Select("SELECT * FROM t_user")
    List<User> selectAll();

    /**
     * 插入用户
     */
    @Insert("INSERT INTO t_user (id, username, password, phone, email, info_id, status, created_at, updated_at) " +
            "VALUES (#{id}, #{username}, #{password}, #{phone}, #{email}, #{infoId}, #{status}, NOW(), NOW())")
    int insert(User user);

    /**
     * 更新用户信息
     */
    @Update("UPDATE t_user SET " +
            "phone = #{phone}, " +
            "email = #{email}, " +
            "info_id = #{infoId}, " +
            "status = #{status}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(User user);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE username = #{username}")
    int countByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM t_user WHERE phone = #{phone}")
    User selectByPhone(String phone);

    /**
     * 检查手机号是否存在
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE phone = #{phone}")
    int countByPhone(String phone);

    /**
     * 根据用户名查询密码（用于验证）
     */
    @Select("SELECT password FROM t_user WHERE username = #{username}")
    String selectPasswordByUsername(String username);
}
