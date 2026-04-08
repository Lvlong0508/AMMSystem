package com.gzasc.aishopping.mapper;

import com.gzasc.aishopping.model.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     */
    @Insert("INSERT INTO t_order (order_id, product_id, quantity, total_price, " +
            "order_status, order_date, contact_id, logistics_id) " +
            "VALUES (#{orderId}, #{productId}, #{quantity}, #{totalPrice}, " +
            "#{orderStatus}, #{orderDate}, #{contactId}, #{logisticsId})")
    int insertOrder(Order order);

    /**
     * 根据订单ID删除订单
     */
    @Delete("DELETE FROM t_order WHERE order_id = #{orderId}")
    int deleteOrderById(String orderId);

    /**
     * 更新订单信息(更新联系人和物流ID)
     */
    @Update("UPDATE t_order SET contact_id = #{contactId}, logistics_id = #{logisticsId} WHERE order_id = #{orderId}")
    int updateOrder(Order order);

    /**
     * 根据订单ID查询订单
     */
    @Select("SELECT o.*, c.name as contact_name, c.phone as contact_phone, c.address as contact_address " +
            "FROM t_order o LEFT JOIN t_contact c ON o.contact_id = c.id " +
            "WHERE o.order_id = #{orderId}")
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

    /**
     * 查询所有订单
     */
    @Select("SELECT o.*, c.name as contact_name, c.phone as contact_phone, c.address as contact_address " +
            "FROM t_order o LEFT JOIN t_contact c ON o.contact_id = c.id")
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

    /**
     * 根据客户名称查询订单
     */
    @Select("SELECT o.*, c.name as contact_name, c.phone as contact_phone, c.address as contact_address " +
            "FROM t_order o LEFT JOIN t_contact c ON o.contact_id = c.id " +
            "WHERE c.name = #{customerName}")
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
    List<Order> selectOrdersByCustomerName(String customerName);

    /**
     * 根据订单状态查询订单
     */
    @Select("SELECT o.*, c.name as contact_name, c.phone as contact_phone, c.address as contact_address " +
            "FROM t_order o LEFT JOIN t_contact c ON o.contact_id = c.id " +
            "WHERE o.order_status = #{status}")
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

    /**
     * 更新订单状态
     */
    @Update("UPDATE t_order SET order_status = #{status} WHERE order_id = #{orderId}")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status);

    /**
     * 更新物流信息ID
     */
    @Update("UPDATE t_order SET logistics_id = #{logisticsId} WHERE order_id = #{orderId}")
    int updateOrderLogisticsId(@Param("orderId") String orderId, @Param("logisticsId") Integer logisticsId);
}
