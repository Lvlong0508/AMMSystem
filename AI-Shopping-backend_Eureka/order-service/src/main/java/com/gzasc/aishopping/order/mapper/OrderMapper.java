package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO t_order (order_id, user_id, shop_id, product_id, quantity, total_price, order_status, order_date, contact_id) " +
            "VALUES (#{orderId}, #{userId}, #{shopId}, #{productId}, #{quantity}, #{totalPrice}, #{orderStatus}, #{orderDate}, #{contactId})")
    int insertOrder(Order order);

    @Delete("DELETE FROM t_order WHERE order_id = #{orderId}")
    int deleteOrderById(@Param("orderId") String orderId);

    @Update("UPDATE t_order SET order_status = #{status} WHERE order_id = #{orderId}")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status);

    @Select("SELECT * FROM t_order WHERE order_id = #{orderId}")
    Order selectOrderById(@Param("orderId") String orderId);

    @Select("SELECT order_id, user_id, shop_id, product_id, quantity, total_price, order_status, order_date FROM t_order WHERE user_id = #{userId}")
    List<Order> selectAbstractOrdersByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_order WHERE user_id = #{userId} AND order_id = #{orderId}")
    Order selectOrderDetailByUser(@Param("userId") Long userId, @Param("orderId") String orderId);

    @Select("SELECT order_id, shop_id, product_id, contact_id, quantity, order_status, order_date FROM t_order WHERE shop_id = #{shopId}")
    List<Order> selectAbstractOrdersByShopId(@Param("shopId") String shopId);

    @Select("SELECT * FROM t_order WHERE shop_id = #{shopId} AND order_id = #{orderId}")
    Order selectOrderDetailByShop(@Param("shopId") String shopId, @Param("orderId") String orderId);
}
