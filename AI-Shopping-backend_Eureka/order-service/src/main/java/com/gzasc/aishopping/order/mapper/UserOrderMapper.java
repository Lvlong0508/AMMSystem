package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.UserOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserOrderMapper {

    @Insert("INSERT INTO t_user_order (user_id, order_id) VALUES (#{userId}, #{orderId})")
    int insert(UserOrder userOrder);

    @Delete("DELETE FROM t_user_order WHERE order_id = #{orderId}")
    int deleteByOrderId(@Param("orderId") String orderId);

    @Select("SELECT order_id FROM t_user_order WHERE user_id = #{userId}")
    List<String> selectOrderIdsByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM t_user_order WHERE user_id = #{userId} AND order_id = #{orderId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "createdAt", column = "created_at")
    })
    UserOrder selectByUserIdAndOrderId(@Param("userId") Integer userId, @Param("orderId") String orderId);

    @Select("SELECT * FROM t_user_order WHERE user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<UserOrder> selectByUserId(@Param("userId") Integer userId);
}