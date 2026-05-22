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
    Merchant selectById(Long id);

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
    @Insert("INSERT INTO t_merchant (id, username, password, phone, email, info_id, status, created_at, updated_at) " +
            "VALUES (#{id}, #{username}, #{password}, #{phone}, #{email}, #{infoId}, #{status}, NOW(), NOW())")
    int insert(Merchant merchant);

    /**
     * 更新商家信息
     */
    @Update("UPDATE t_merchant SET " +
            "phone = #{phone}, " +
            "email = #{email}, " +
            "info_id = #{infoId}, " +
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
     * 根据ID更新商家（用于更新 infoId）
     */
    @Update("UPDATE t_merchant SET info_id = #{infoId}, updated_at = NOW() WHERE id = #{id}")
    int updateById(Merchant merchant);

    /**
     * 根据用户名查询密码（用于验证）
     */
    @Select("SELECT password FROM t_merchant WHERE username = #{username}")
    String selectPasswordByUsername(String username);
}
