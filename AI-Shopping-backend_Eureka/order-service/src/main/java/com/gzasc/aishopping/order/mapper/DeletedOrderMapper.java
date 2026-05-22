package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.DeletedOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeletedOrderMapper {

    @Insert("INSERT INTO deleted_orders (order_id, product_id, quantity, total_price, " +
            "order_status, order_date, contact_id, deleted_at) " +
            "VALUES (#{orderId}, #{productId}, #{quantity}, #{totalPrice}, " +
            "#{orderStatus}, #{orderDate}, #{contactId}, #{deletedAt})")
    int insertDeletedOrder(DeletedOrder deletedOrder);

    @Select("SELECT * FROM deleted_orders WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "deletedAt", column = "deleted_at")
    })
    DeletedOrder selectDeletedOrderById(Integer id);

    @Select("SELECT * FROM deleted_orders ORDER BY deleted_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "deletedAt", column = "deleted_at")
    })
    List<DeletedOrder> selectAllDeletedOrders();

    @Select("SELECT * FROM deleted_orders WHERE order_id = #{orderId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "deletedAt", column = "deleted_at")
    })
    DeletedOrder selectDeletedOrderByOrderId(String orderId);
}
