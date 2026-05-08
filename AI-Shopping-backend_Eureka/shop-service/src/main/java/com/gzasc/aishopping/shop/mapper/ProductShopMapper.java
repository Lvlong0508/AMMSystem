package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.ProductShop;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductShopMapper {

    @Select("SELECT * FROM product_shops WHERE id = #{id}")
    ProductShop selectById(@Param("id") String id);

    @Select("SELECT * FROM product_shops WHERE shop_id = #{shopId}")
    List<ProductShop> selectByShopId(@Param("shopId") String shopId);

    @Select("SELECT * FROM product_shops WHERE product_id = #{productId}")
    List<ProductShop> selectByProductId(@Param("productId") String productId);

    @Insert("INSERT INTO product_shops (id, product_id, shop_id, created_at) " +
            "VALUES (#{id}, #{productId}, #{shopId}, NOW())")
    int insert(ProductShop productShop);

    @Delete("DELETE FROM product_shops WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    @Delete("DELETE FROM product_shops WHERE shop_id = #{shopId} AND product_id = #{productId}")
    int deleteByShopAndProduct(@Param("shopId") String shopId, @Param("productId") String productId);

    @Select("SELECT shop_id FROM product_shops WHERE product_id = #{productId} LIMIT 1")
    String selectShopIdByProductId(@Param("productId") String productId);
}