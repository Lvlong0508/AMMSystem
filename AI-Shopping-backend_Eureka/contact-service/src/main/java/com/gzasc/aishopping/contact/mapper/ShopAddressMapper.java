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

    /**
     * 公共结果映射
     */
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

    /**
     * 根据ID查询地址
     * @param id 地址ID
     * @return 地址实体
     */
    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id = #{id}")
    @ResultMap("SHOP_ADDRESS_MAPPING")
    ShopAddress selectAddressById(int id);

    /**
     * 根据店铺ID查询所有地址
     * @param shopId 店铺ID
     * @return 地址列表
     */
    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId})")
    @ResultMap("SHOP_ADDRESS_MAPPING")
    List<ShopAddress> selectAddressesByShopId(String shopId);

    /**
     * 根据店铺ID查询收货地址（address_type = 1）
     * @param shopId 店铺ID
     * @return 收货地址列表
     */
    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId}) " +
            "AND address_type = 1")
    @ResultMap("SHOP_ADDRESS_MAPPING")
    List<ShopAddress> selectShipAddressesByShopId(String shopId);

    /**
     * 插入地址
     * @param address 地址实体
     * @return 影响行数
     */
    @Insert("INSERT INTO shop_address (name, phone, address, address_type, is_default) " +
            "VALUES (#{name}, #{phone}, #{address}, #{addressType}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAddress(ShopAddress address);

    /**
     * 更新地址
     * @param address 地址实体
     * @return 影响行数
     */
    @Update("UPDATE shop_address SET name = #{name}, phone = #{phone}, address = #{address}, " +
            "address_type = #{addressType}, is_default = #{isDefault} WHERE id = #{id}")
    int updateAddress(ShopAddress address);

    /**
     * 根据ID删除地址
     * @param id 地址ID
     * @return 影响行数
     */
    @Delete("DELETE FROM shop_address WHERE id = #{id}")
    int deleteAddressById(int id);

    /**
     * 清除指定类型地址的默认状态
     * @param shopId 店铺ID
     * @param addressType 地址类型
     * @return 影响行数
     */
    @Update("UPDATE shop_address SET is_default = 0 WHERE address_type = #{addressType} " +
            "AND id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId})")
    int clearDefaultByType(@Param("shopId") String shopId, @Param("addressType") int addressType);

    /**
     * 设置地址为默认
     * @param id 地址ID
     * @return 影响行数
     */
    @Update("UPDATE shop_address SET is_default = 1 WHERE id = #{id}")
    int setDefaultById(int id);
}