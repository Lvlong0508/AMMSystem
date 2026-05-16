package com.gzasc.aishopping.contact.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ShopAddressRelMapper {

    @Insert("INSERT INTO shop_address_rel (shop_id, address_id) VALUES (#{shopId}, #{addressId})")
    int insertRel(@Param("shopId") String shopId, @Param("addressId") int addressId);

    @Delete("DELETE FROM shop_address_rel WHERE address_id = #{addressId}")
    int deleteRelByAddressId(int addressId);

    @Select("SELECT shop_id FROM shop_address_rel WHERE address_id = #{addressId}")
    String selectShopIdByAddressId(int addressId);
}