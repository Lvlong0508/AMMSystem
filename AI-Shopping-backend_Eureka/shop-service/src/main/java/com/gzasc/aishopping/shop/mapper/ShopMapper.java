package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.Shop;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ShopMapper {

    @Select("SELECT * FROM shops LIMIT 20 OFFSET #{offset}")
    List<Shop> selectShopsByPage(@Param("offset") int offset);

    @Select("SELECT * FROM shops WHERE id = #{id}")
    Shop selectShopById(@Param("id") Long id);

    @Select("SELECT * FROM shops WHERE merchant_id = #{merchantId}")
    List<Shop> selectShopsByMerchantId(@Param("merchantId") Long merchantId);

    @Select("SELECT s.* FROM shops s " +
            "INNER JOIN merchant_roles mr ON s.id = mr.shop_id " +
            "WHERE mr.merchant_id = #{userId} AND s.status = 1")
    List<Shop> selectShopsByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO shops (id, merchant_id, shop_info_id, status, created_at, updated_at) " +
            "VALUES (#{id}, #{merchantId}, #{shopInfoId}, #{status}, NOW(), NOW())")
    int insertShop(Shop shop);

    @Update("UPDATE shops SET shop_info_id = #{shopInfoId}, " +
            "status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateShop(Shop shop);

    @Delete("DELETE FROM shops WHERE id = #{id}")
    int deleteShop(@Param("id") Long id);

    @Update("UPDATE shops SET status = 0, updated_at = NOW() WHERE id = #{id}")
    int closeShop(@Param("id") Long id);

    @Update("UPDATE shops SET status = 1, updated_at = NOW() WHERE id = #{id}")
    int openShop(@Param("id") Long id);

    @Select("SELECT * FROM shops WHERE status = 1 LIMIT #{size} OFFSET #{offset}")
    List<Shop> selectActiveShops(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM shops WHERE status = 1")
    int countActiveShops();

    @Select({"<script>",
             "SELECT * FROM shops",
             "<if test='ids != null and ids.size() > 0'> WHERE id IN",
             "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if><if test='ids == null or ids.size() == 0'> WHERE 1=0</if>",
             "</script>"})
    List<Shop> selectShopsByIds(@Param("ids") Collection<Long> ids);
}