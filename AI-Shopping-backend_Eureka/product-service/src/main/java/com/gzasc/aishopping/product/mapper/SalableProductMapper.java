package com.gzasc.aishopping.product.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SalableProductMapper {

    @Insert("INSERT INTO salable_products (id) VALUES (#{productId})")
    int addSalable(@Param("productId") Long productId);

    @Delete("DELETE FROM salable_products WHERE id = #{productId}")
    int removeSalable(@Param("productId") Long productId);

    @Select("SELECT id FROM salable_products LIMIT #{limit} OFFSET #{offset}")
    List<Long> selectAll(@Param("offset") int offset, @Param("limit") int limit);
}