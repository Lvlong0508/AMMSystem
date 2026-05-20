package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.ShopAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 店铺地址 Mapper 接口
 * 对应 shop_address 表，处理店铺收货/发货地址
 */
@Mapper
public interface ShopAddressMapper {

    /* ========== 公共结果映射 ========== */

    @Results(id = "SHOP_ADDRESS_MAPPING", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "addressType", column = "address_type"),
            @Result(property = "isDefault", column = "is_default"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })

    /* ========== 查询操作 ========== */

    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id = #{id}")
    @ResultMap("SHOP_ADDRESS_MAPPING")
    ShopAddress selectAddressById(int id);

    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId})")
    @ResultMap("SHOP_ADDRESS_MAPPING")
    List<ShopAddress> selectAddressesByShopId(String shopId);

    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId}) " +
            "AND address_type = 1 AND is_default = 1")
    @ResultMap("SHOP_ADDRESS_MAPPING")
    ShopAddress selectDefaultShipAddressByShopId(String shopId);

    /* ========== 地址 CRUD ========== */

    @Insert("INSERT INTO shop_address (name, phone, address, address_type, is_default) " +
            "VALUES (#{name}, #{phone}, #{address}, #{addressType}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAddress(ShopAddress address);

    @Update("UPDATE shop_address SET name = #{name}, phone = #{phone}, address = #{address}, " +
            "address_type = #{addressType}, is_default = #{isDefault} WHERE id = #{id}")
    int updateAddress(ShopAddress address);

    @Delete("DELETE FROM shop_address WHERE id = #{id}")
    int deleteAddressById(int id);

    @Update("UPDATE shop_address SET is_default = 0 WHERE address_type = #{addressType} " +
            "AND id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId})")
    int clearDefaultByType(@Param("shopId") String shopId, @Param("addressType") int addressType);

    @Update("UPDATE shop_address SET is_default = 1 WHERE id = #{id}")
    int setDefaultById(int id);

    /* ========== 店铺-地址关联操作 ========== */

    @Insert("INSERT INTO shop_address_rel (shop_id, address_id) VALUES (#{shopId}, #{addressId})")
    int insertRel(@Param("shopId") String shopId, @Param("addressId") int addressId);

    @Delete("DELETE FROM shop_address_rel WHERE address_id = #{addressId}")
    int deleteRelByAddressId(int addressId);

    @Select("SELECT shop_id FROM shop_address_rel WHERE address_id = #{addressId}")
    String selectShopIdByAddressId(int addressId);
}