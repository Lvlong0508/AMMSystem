package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO t_order (order_id, product_id, quantity, total_price, " +
            "order_status, order_date, contact_id, logistics_id) " +
            "VALUES (#{orderId}, #{productId}, #{quantity}, #{totalPrice}, " +
            "#{orderStatus}, #{orderDate}, #{contactId}, #{logisticsId})")
    int insertOrder(Order order);

    @Delete("DELETE FROM t_order WHERE order_id = #{orderId}")
    int deleteOrderById(String orderId);

    @Update("UPDATE t_order SET contact_id = #{contactId}, logistics_id = #{logisticsId} WHERE order_id = #{orderId}")
    int updateOrder(Order order);

    @Select("SELECT * FROM t_order WHERE order_id = #{orderId}")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "logisticsId", column = "logistics_id")
    })
    Order selectOrderById(String orderId);

    @Select("SELECT * FROM t_order")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "logisticsId", column = "logistics_id")
    })
    List<Order> selectAllOrders();

    @Select("SELECT * FROM t_order WHERE order_status = #{status}")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "logisticsId", column = "logistics_id")
    })
    List<Order> selectOrdersByStatus(String status);

    @Update("UPDATE t_order SET order_status = #{status} WHERE order_id = #{orderId}")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status);

    @Update("UPDATE t_order SET logistics_id = #{logisticsId} WHERE order_id = #{orderId}")
    int updateOrderLogisticsId(@Param("orderId") String orderId, @Param("logisticsId") Integer logisticsId);
}
