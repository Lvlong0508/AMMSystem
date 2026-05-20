package com.gzasc.aishopping.product.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SalableProductMapper {

    @Insert("INSERT INTO salable_products (id) VALUES (#{productId})")
    int addSalable(@Param("productId") String productId);

    @Delete("DELETE FROM salable_products WHERE id = #{productId}")
    int removeSalable(@Param("productId") String productId);

    @Select("SELECT COUNT(*) > 0 FROM salable_products WHERE id = #{productId}")
    boolean isSalable(@Param("productId") String productId);

    @Select("SELECT id FROM salable_products")
    List<String> selectAll();
}