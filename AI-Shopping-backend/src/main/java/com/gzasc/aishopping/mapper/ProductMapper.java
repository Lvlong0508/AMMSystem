package com.gzasc.aishopping.mapper;

import com.gzasc.aishopping.model.Product;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM products LIMIT 20 OFFSET #{offset}")
    List<Product> selectProductsByPage(@Param("offset") int offset);

    @Select("SELECT * FROM products WHERE id = #{id}")
    Product selectProductById(String id);

    /**
     * 根据商品名称查询商品（模糊查询）
     */
    @Select("SELECT * FROM products WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<Product> selectProductsByName(@Param("name") String name);

    /**
     * 扣减商品库存
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 影响的行数
     */
    @Update("UPDATE products SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") String productId, @Param("quantity") int quantity);

    /**
     * 恢复商品库存
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 影响的行数
     */
    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") String productId, @Param("quantity") int quantity);

    /**
     * 插入商品
     */
    @Insert("INSERT INTO products (id, name, price, tags, description, stock, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, #{price}, #{tags}, #{description}, #{stock}, NOW(), NOW())")
    int insertProduct(Product product);

    /**
     * 删除商品
     */
    @Delete("DELETE FROM products WHERE id = #{productId}")
    int deleteProduct(@Param("productId") String productId);

    /**
     * 更新商品
     */
    @Update("UPDATE products SET name = #{name}, price = #{price}, tags = #{tags}, " +
            "description = #{description}, stock = #{stock}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateProduct(Product product);
}