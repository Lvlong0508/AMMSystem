package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.Product;
import org.apache.ibatis.annotations.*;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM products LIMIT 20 OFFSET #{offset}")
    List<Product> selectProductsByPage(@Param("offset") int offset);

    @Select("SELECT * FROM products WHERE id = #{id}")
    Product selectProductById(String id);

    @Select("SELECT * FROM products WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<Product> selectProductsByName(@Param("name") String name);

    @Update("UPDATE products SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") String productId, @Param("quantity") int quantity);

    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") String productId, @Param("quantity") int quantity);

    @Insert("INSERT INTO products (id, name, price, tags, description, stock, is_sale, image_id, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, #{price}, #{tags}, #{description}, #{stock}, #{isSale}, #{imageId}, NOW(), NOW())")
    int insertProduct(Product product);

    @Delete("DELETE FROM products WHERE id = #{productId}")
    int deleteProduct(@Param("productId") String productId);

    @Update("UPDATE products SET name = #{name}, price = #{price}, tags = #{tags}, " +
            "description = #{description}, stock = #{stock}, is_sale = #{isSale}, image_id = #{imageId}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateProduct(Product product);

    @Select("<script>" +
            "SELECT * FROM products WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Product> selectProductsByIds(@Param("ids") List<String> ids);

    @Select("SELECT * FROM products WHERE is_sale = #{isSale}")
    List<Product> selectBySaleStatus(@Param("isSale") boolean isSale);

    @Select("SELECT * FROM products WHERE price BETWEEN #{minPrice} AND #{maxPrice}")
    List<Product> selectByPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);

    @Select("SELECT * FROM products WHERE is_sale = 1 LIMIT #{limit}, 10")
    List<Product> selectSalableProducts(@Param("limit") int limit);

    @Select("SELECT * FROM products WHERE price BETWEEN #{minPrice} AND #{maxPrice} LIMIT #{limit}, 10")
    List<Product> selectByPriceRangeWithPage(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, @Param("limit") int limit);
}