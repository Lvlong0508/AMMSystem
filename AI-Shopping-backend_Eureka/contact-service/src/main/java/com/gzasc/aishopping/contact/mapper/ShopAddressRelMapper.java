package com.gzasc.aishopping.contact.mapper;

import org.apache.ibatis.annotations.*;

/**
 * 店铺-地址关联 Mapper 接口
 * 对应 shop_address_rel 表，处理店铺与地址的关联关系
 */
@Mapper
public interface ShopAddressRelMapper {

    /**
     * 插入店铺-地址关联记录
     * @param shopId 店铺ID
     * @param addressId 地址ID
     * @return 影响行数
     */
    @Insert("INSERT INTO shop_address_rel (shop_id, address_id) VALUES (#{shopId}, #{addressId})")
    int insertRel(@Param("shopId") String shopId, @Param("addressId") int addressId);

    /**
     * 根据地址ID删除关联记录
     * @param addressId 地址ID
     * @return 影响行数
     */
    @Delete("DELETE FROM shop_address_rel WHERE address_id = #{addressId}")
    int deleteRelByAddressId(int addressId);

    /**
     * 根据地址ID查询店铺ID
     * @param addressId 地址ID
     * @return 店铺ID
     */
    @Select("SELECT shop_id FROM shop_address_rel WHERE address_id = #{addressId}")
    String selectShopIdByAddressId(int addressId);
}