package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.Product;
import org.apache.ibatis.annotations.*;

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

    @Insert("INSERT INTO products (id, name, price, tags, description, stock, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, #{price}, #{tags}, #{description}, #{stock}, NOW(), NOW())")
    int insertProduct(Product product);

    @Delete("DELETE FROM products WHERE id = #{productId}")
    int deleteProduct(@Param("productId") String productId);

    @Update("UPDATE products SET name = #{name}, price = #{price}, tags = #{tags}, " +
            "description = #{description}, stock = #{stock}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateProduct(Product product);
}
