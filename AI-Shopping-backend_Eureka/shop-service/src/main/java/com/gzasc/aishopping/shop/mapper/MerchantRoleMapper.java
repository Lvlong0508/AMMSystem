package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.MerchantRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MerchantRoleMapper {

    @Select("SELECT * FROM merchant_roles WHERE id = #{id}")
    MerchantRole selectById(@Param("id") Long id);

    @Select("SELECT * FROM merchant_roles WHERE merchant_id = #{merchantId}")
    List<MerchantRole> selectByMerchantId(@Param("merchantId") Long merchantId);

    @Select("SELECT * FROM merchant_roles WHERE shop_id = #{shopId}")
    List<MerchantRole> selectByShopId(@Param("shopId") Long shopId);

    @Insert("INSERT INTO merchant_roles (merchant_id, shop_id, role, assigned_by, created_at) " +
            "VALUES (#{merchantId}, #{shopId}, #{role}, #{assignedBy}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MerchantRole merchantRole);

    @Update("UPDATE merchant_roles SET role = #{role} WHERE id = #{id}")
    int updateRole(MerchantRole merchantRole);

    @Delete("DELETE FROM merchant_roles WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT * FROM merchant_roles WHERE merchant_id = #{merchantId} AND shop_id = #{shopId} LIMIT 1")
    MerchantRole selectByMerchantAndShop(@Param("merchantId") Long merchantId, @Param("shopId") Long shopId);

    @Select("SELECT * FROM merchant_roles WHERE merchant_id = #{merchantId} AND shop_id = #{shopId} AND role = #{role} LIMIT 1")
    MerchantRole selectByMerchantShopAndRole(@Param("merchantId") Long merchantId, @Param("shopId") Long shopId, @Param("role") Integer role);

    @Delete("DELETE FROM merchant_roles WHERE merchant_id = #{merchantId} AND shop_id = #{shopId}")
    int deleteByMerchantAndShop(@Param("merchantId") Long merchantId, @Param("shopId") Long shopId);
}