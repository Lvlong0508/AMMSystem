package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.ProductImageInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductImageInfoMapper {

    @Insert("INSERT INTO product_images (url) VALUES (#{url})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductImageInfo image);

    @Delete("DELETE FROM product_images WHERE id = #{id}")
    int deleteById(@Param("id") int id);

    @Select("SELECT * FROM product_images WHERE id = #{id}")
    ProductImageInfo selectById(@Param("id") int id);

    @Select("<script>" +
            "SELECT * FROM product_images WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<ProductImageInfo> selectByIds(@Param("ids") List<Integer> ids);

    @Update("UPDATE product_images SET url = #{url} WHERE id = #{id}")
    int updateUrl(ProductImageInfo image);
}