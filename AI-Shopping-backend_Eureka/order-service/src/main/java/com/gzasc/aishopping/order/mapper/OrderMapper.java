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

    @Select("SELECT * FROM t_order WHERE shop_id = #{shopId} AND order_status = 'PAID'")
    List<Order> selectPaidOrdersByShopId(@Param("shopId") String shopId);

    @Select("SELECT * FROM t_order WHERE order_status = 'PENDING' AND order_date < NOW() - INTERVAL #{minutes} MINUTE")
    List<Order> selectExpiredPendingOrders(@Param("minutes") int minutes);

    @Update("UPDATE t_order SET order_status = #{newStatus} WHERE order_id = #{orderId} AND order_status = #{oldStatus}")
    int updateOrderStatusCas(@Param("orderId") String orderId,
                             @Param("newStatus") String newStatus,
                             @Param("oldStatus") String oldStatus);

    @Update({"<script>",
             "UPDATE t_order SET order_status = #{newStatus}",
             "WHERE order_id = #{orderId}",
             "<if test='expectedStatuses != null and expectedStatuses.size() > 0'>",
             "AND order_status IN",
             "<foreach collection='expectedStatuses' item='s' open='(' separator=',' close=')'>#{s}</foreach>",
             "</if>",
             "</script>"})
    int updateOrderStatusCasMulti(@Param("orderId") String orderId,
                                  @Param("newStatus") String newStatus,
                                  @Param("expectedStatuses") List<String> expectedStatuses);
}
