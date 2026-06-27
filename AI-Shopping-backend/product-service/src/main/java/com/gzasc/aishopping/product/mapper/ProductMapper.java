package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.Product;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ProductMapper {

    // 先获取抽象信息
    @Select("<script>" +
            "SELECT id,name,price,tags,image_id AS imageId,shop_id AS shopId FROM products WHERE is_sale = 1 AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Product> selectAbstractProductsByIds(@Param("ids") List<Long> ids);

    // 再按id获取详情
    @Select("SELECT * FROM products WHERE id = #{id}")
    Product selectProductById(Long id);

    @Select("SELECT * FROM products WHERE name LIKE CONCAT('%', #{name}, '%') AND is_sale = 1")
    List<Product> selectProductsByName(@Param("name") String name);

    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE products SET is_sale = #{isSale}, updated_at = NOW() WHERE id = #{id}")
    int updateSaleStatus(@Param("id") Long id, @Param("isSale") boolean isSale);

    @Insert("INSERT INTO products (id, name, price, tags, description, stock, is_sale, image_id, shop_id, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, #{price}, #{tags}, #{description}, #{stock}, #{isSale}, #{imageId}, #{shopId}, NOW(), NOW())")
    int insertProduct(Product product);

    @Delete("DELETE FROM products WHERE id = #{productId}")
    int deleteProduct(@Param("productId") Long productId);

    @Update("<script>" +
            "UPDATE products SET updated_at = NOW()" +
            "<if test='name != null'>, name = #{name}</if>" +
            "<if test='price != null'>, price = #{price}</if>" +
            "<if test='tags != null'>, tags = #{tags}</if>" +
            "<if test='description != null'>, description = #{description}</if>" +
            "<if test='stock != null'>, stock = #{stock}</if>" +
            "<if test='imageId != null'>, image_id = #{imageId}</if>" +
            "<if test='shopId != null'>, shop_id = #{shopId}</if>" +
            " WHERE id = #{id}" +
            "</script>")
    int updateProduct(Product product);

    // 提供给商家端的分页抽象查询接口
    @Select("<script>" +
            "SELECT id,name,price,stock,tags,is_sale AS isSale,image_id AS imageId,shop_id AS shopId FROM products WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Product> selectAbstractProductsByIdsJustMerchant(@Param("ids") List<Long> ids);

    @Select("SELECT id,name,price,stock,tags,is_sale AS isSale,image_id AS imageId,shop_id AS shopId FROM products WHERE shop_id = #{shopId}")
    List<Product> selectByShopId(@Param("shopId") Long shopId);

    @Select("SELECT id,name,price,stock,tags,image_id AS imageId,shop_id AS shopId FROM products WHERE shop_id = #{shopId} AND is_sale = 1")
    List<Product> selectSalableByShopId(@Param("shopId") Long shopId);

    @Select("SELECT id,name,price,stock,image_id AS imageId FROM products WHERE is_sale = 1 LIMIT #{limit} OFFSET #{offset}")
    List<Product> selectCardProductsPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT id,name,price,tags,image_id AS imageId,shop_id AS shopId FROM products WHERE price BETWEEN #{minPrice} AND #{maxPrice} AND is_sale = 1 LIMIT 20 OFFSET #{offset}")
    List<Product> selectByPriceRangeWithPage(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, @Param("offset") int offset);

}
