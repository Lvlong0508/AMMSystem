package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.Shop;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShopMapper {

    @Select("SELECT * FROM shops LIMIT 20 OFFSET #{offset}")
    List<Shop> selectShopsByPage(@Param("offset") int offset);

    @Select("SELECT * FROM shops WHERE id = #{id}")
    Shop selectShopById(@Param("id") String id);

    @Select("SELECT * FROM shops WHERE merchant_id = #{merchantId}")
    List<Shop> selectShopsByMerchantId(@Param("merchantId") String merchantId);

    @Select("SELECT s.* FROM shops s " +
            "INNER JOIN merchant_roles mr ON s.id = mr.shop_id " +
            "WHERE mr.merchant_id = #{userId} AND s.status = 1")
    List<Shop> selectShopsByUserId(@Param("userId") String userId);

    @Insert("INSERT INTO shops (id, merchant_id, name, description, logo_id, status, created_at, updated_at) " +
            "VALUES (#{id}, #{merchantId}, #{name}, #{description}, #{logoId}, #{status}, NOW(), NOW())")
    int insertShop(Shop shop);

    @Update("UPDATE shops SET name = #{name}, description = #{description}, logo_id = #{logoId}, " +
            "status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateShop(Shop shop);

    @Delete("DELETE FROM shops WHERE id = #{id}")
    int deleteShop(@Param("id") String id);

    @Update("UPDATE shops SET status = 0, updated_at = NOW() WHERE id = #{id}")
    int closeShop(@Param("id") String id);

    @Select("SELECT * FROM shops WHERE status = 1 LIMIT #{size} OFFSET #{offset}")
    List<Shop> selectActiveShops(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM shops WHERE status = 1")
    int countActiveShops();
}