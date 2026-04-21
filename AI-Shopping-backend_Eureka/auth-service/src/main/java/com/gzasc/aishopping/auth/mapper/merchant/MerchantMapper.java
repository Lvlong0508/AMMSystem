package com.gzasc.aishopping.auth.mapper.merchant;

import com.gzasc.aishopping.auth.model.Merchant;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商家 Mapper 接口
 * 使用 merchant 数据源（eureka_product 库）
 */
@Mapper
public interface MerchantMapper {

    /**
     * 根据ID查询商家
     */
    @Select("SELECT * FROM t_merchant WHERE id = #{id}")
    Merchant selectById(Integer id);

    /**
     * 根据用户名查询商家
     */
    @Select("SELECT * FROM t_merchant WHERE username = #{username}")
    Merchant selectByUsername(String username);

    /**
     * 查询所有商家
     */
    @Select("SELECT * FROM t_merchant")
    List<Merchant> selectAll();

    /**
     * 插入商家
     */
    @Insert("INSERT INTO t_merchant (username, password, shop_name, phone, email, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{shopName}, #{phone}, #{email}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Merchant merchant);

    /**
     * 更新商家信息
     */
    @Update("UPDATE t_merchant SET " +
            "shop_name = #{shopName}, " +
            "phone = #{phone}, " +
            "email = #{email}, " +
            "status = #{status}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(Merchant merchant);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) FROM t_merchant WHERE username = #{username}")
    int countByUsername(String username);

    /**
     * 检查手机号是否存在
     */
    @Select("SELECT COUNT(*) FROM t_merchant WHERE phone = #{phone}")
    int countByPhone(String phone);

    /**
     * 根据用户名查询密码（用于验证）
     */
    @Select("SELECT password FROM t_merchant WHERE username = #{username}")
    String selectPasswordByUsername(String username);
}
