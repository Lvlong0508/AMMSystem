package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.Product;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ProductMapper {

    // 先获取抽象信息
    @Select("<script>" +
            "SELECT id,name,price,tags,image_id AS imageId,shop_id AS shopId FROM products WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Product> selectAbstractProductsByIds(@Param("ids") List<String> ids);

    // 再按id获取详情
    @Select("SELECT * FROM products WHERE id = #{id}")
    Product selectProductById(String id);

    @Select("SELECT * FROM products WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<Product> selectProductsByName(@Param("name") String name);

    @Update("UPDATE products SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE products SET is_sale = #{isSale}, updated_at = NOW() WHERE id = #{id}")
    int updateSaleStatus(@Param("id") Long id, @Param("isSale") boolean isSale);

    @Insert("INSERT INTO products (id, name, price, tags, description, stock, is_sale, image_id, shop_id, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, #{price}, #{tags}, #{description}, #{stock}, #{isSale}, #{imageId}, #{shopId}, NOW(), NOW())")
    int insertProduct(Product product);

    @Delete("DELETE FROM products WHERE id = #{productId}")
    int deleteProduct(@Param("productId") Long productId);

    @Update("UPDATE products SET name = #{name}, price = #{price}, tags = #{tags}, " +
            "description = #{description}, stock = #{stock}, is_sale = #{isSale}, image_id = #{imageId}, shop_id = #{shopId}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateProduct(Product product);

    @Select("SELECT id,name,price,tags,image_id AS imageId,shop_id AS shopId FROM products WHERE price BETWEEN #{minPrice} AND #{maxPrice}")
    List<Product> selectByPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);

    // 提供给商家端的分页抽象查询接口
    @Select("<script>" +
            "SELECT id,name,price,tags,is_sale AS isSale,image_id AS imageId,shop_id AS shopId FROM products WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Product> selectAbstractProductsByIdsJustMerchant(@Param("ids") List<String> ids);

    @Select("SELECT id,name,price,tags,image_id AS imageId,shop_id AS shopId FROM products WHERE price BETWEEN #{minPrice} AND #{maxPrice} LIMIT 20 OFFSET #{offset}")
    List<Product> selectByPriceRangeWithPage(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, @Param("offset") int offset);

    @Update("UPDATE products SET image_id = #{imageId}, updated_at = NOW() WHERE id = #{id}")
    int updateProductImageId(@Param("id") Long id, @Param("imageId") int imageId);

    @Select("SELECT * FROM products WHERE shop_id = #{shopId} ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Product> selectByShopId(@Param("shopId") Long shopId, @Param("offset") int offset, @Param("size") int size);
}