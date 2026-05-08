package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.OrderShop;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderShopMapper {

    @Select("SELECT * FROM order_shops WHERE id = #{id}")
    OrderShop selectById(@Param("id") String id);

    @Select("SELECT * FROM order_shops WHERE shop_id = #{shopId}")
    List<OrderShop> selectByShopId(@Param("shopId") String shopId);

    @Select("SELECT * FROM order_shops WHERE order_id = #{orderId}")
    List<OrderShop> selectByOrderId(@Param("orderId") String orderId);

    @Insert("INSERT INTO order_shops (id, order_id, shop_id, created_at) " +
            "VALUES (#{id}, #{orderId}, #{shopId}, NOW())")
    int insert(OrderShop orderShop);

    @Delete("DELETE FROM order_shops WHERE id = #{id}")
    int deleteById(@Param("id") String id);
}