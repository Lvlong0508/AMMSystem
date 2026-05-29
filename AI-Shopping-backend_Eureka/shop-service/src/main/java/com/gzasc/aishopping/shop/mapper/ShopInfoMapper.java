package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.ShopInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShopInfoMapper {

    @Select("SELECT * FROM shop_info WHERE id = #{id}")
    ShopInfo selectById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT * FROM shop_info" +
            "<if test='ids != null and ids.size() > 0'> WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</if><if test='ids == null or ids.size() == 0'> WHERE 1=0</if>" +
            "</script>")
    List<ShopInfo> selectBatch(@Param("ids") List<Long> ids);

    @Insert("INSERT INTO shop_info (id, name, description, logourl, address, phone) " +
            "VALUES (#{id}, #{name}, #{description}, #{logoUrl}, #{address}, #{phone})")
    int insert(ShopInfo shopInfo);

    @Update("UPDATE shop_info SET name = #{name}, description = #{description}, " +
            "logourl = #{logoUrl}, address = #{address}, phone = #{phone} WHERE id = #{id}")
    int update(ShopInfo shopInfo);
}
