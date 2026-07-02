package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.ReturnRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReturnRequestMapper {

    @Insert("INSERT INTO return_requests (order_id, user_id, shop_id, return_reason, status, created_date) " +
            "VALUES (#{orderId}, #{userId}, #{shopId}, #{returnReason}, #{status}, #{createdDate})")
    int insert(ReturnRequest request);

    @Select("SELECT * FROM return_requests WHERE order_id = #{orderId}")
    ReturnRequest selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM return_requests WHERE order_id = #{orderId} AND user_id = #{userId}")
    ReturnRequest selectByOrderIdAndUser(@Param("orderId") String orderId, @Param("userId") Long userId);

    @Select("SELECT * FROM return_requests WHERE user_id = #{userId} ORDER BY created_date DESC")
    List<ReturnRequest> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM return_requests WHERE order_id = #{orderId} AND shop_id = #{shopId}")
    ReturnRequest selectByOrderIdAndShop(@Param("orderId") String orderId, @Param("shopId") String shopId);

    @Select("SELECT * FROM return_requests WHERE shop_id = #{shopId} AND status = #{status} ORDER BY created_date DESC")
    List<ReturnRequest> selectByShopAndStatus(@Param("shopId") String shopId, @Param("status") String status);

    @Update("UPDATE return_requests SET status = #{status}, updated_date = CURRENT_TIMESTAMP WHERE order_id = #{orderId} AND status = 'applying'")
    int updateStatus(@Param("orderId") String orderId, @Param("status") String status);

    @Update("UPDATE return_requests SET logistics_id = #{logisticsId}, updated_date = CURRENT_TIMESTAMP WHERE order_id = #{orderId} AND status = 'agreed' AND logistics_id IS NULL")
    int updateLogisticsId(@Param("orderId") String orderId, @Param("logisticsId") Integer logisticsId);

    @Delete("DELETE FROM return_requests WHERE order_id = #{orderId} AND user_id = #{userId}")
    int deleteByOrderIdAndUser(@Param("orderId") String orderId, @Param("userId") Long userId);
}
