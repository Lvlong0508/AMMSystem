package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.MerchantRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MerchantRoleMapper {

    @Select("SELECT * FROM merchant_roles WHERE id = #{id}")
    MerchantRole selectById(@Param("id") String id);

    @Select("SELECT * FROM merchant_roles WHERE merchant_id = #{merchantId}")
    List<MerchantRole> selectByMerchantId(@Param("merchantId") String merchantId);

    @Select("SELECT * FROM merchant_roles WHERE shop_id = #{shopId}")
    List<MerchantRole> selectByShopId(@Param("shopId") String shopId);

    @Insert("INSERT INTO merchant_roles (id, merchant_id, shop_id, role, assigned_by, created_at) " +
            "VALUES (#{id}, #{merchantId}, #{shopId}, #{role}, #{assignedBy}, NOW())")
    int insert(MerchantRole merchantRole);

    @Update("UPDATE merchant_roles SET role = #{role} WHERE id = #{id}")
    int updateRole(MerchantRole merchantRole);

    @Delete("DELETE FROM merchant_roles WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    @Select("SELECT * FROM merchant_roles WHERE merchant_id = #{merchantId} AND shop_id = #{shopId} LIMIT 1")
    MerchantRole selectByMerchantAndShop(@Param("merchantId") String merchantId, @Param("shopId") String shopId);

    @Select("SELECT * FROM merchant_roles WHERE merchant_id = #{merchantId} AND shop_id = #{shopId} AND role = #{role} LIMIT 1")
    MerchantRole selectByMerchantShopAndRole(@Param("merchantId") String merchantId, @Param("shopId") String shopId, @Param("role") String role);
}