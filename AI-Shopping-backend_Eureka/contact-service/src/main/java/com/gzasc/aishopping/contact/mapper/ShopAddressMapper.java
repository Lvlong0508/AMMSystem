package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.ShopAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShopAddressMapper {

    @Insert("INSERT INTO shop_address (name, phone, address, address_type, is_default) " +
            "VALUES (#{name}, #{phone}, #{address}, #{addressType}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAddress(ShopAddress address);

    @Delete("DELETE FROM shop_address WHERE id = #{id}")
    int deleteAddressById(int id);

    @Update("UPDATE shop_address SET name = #{name}, phone = #{phone}, address = #{address}, " +
            "address_type = #{addressType}, is_default = #{isDefault} WHERE id = #{id}")
    int updateAddress(ShopAddress address);

    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "addressType", column = "address_type"),
            @Result(property = "isDefault", column = "is_default"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    ShopAddress selectAddressById(int id);

    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId})")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "addressType", column = "address_type"),
            @Result(property = "isDefault", column = "is_default"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<ShopAddress> selectAddressesByShopId(String shopId);

    @Update("UPDATE shop_address SET is_default = 0 WHERE address_type = #{addressType} " +
            "AND id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId})")
    int clearDefaultByType(@Param("shopId") String shopId, @Param("addressType") int addressType);

    @Update("UPDATE shop_address SET is_default = 1 WHERE id = #{id}")
    int setDefaultById(int id);
}