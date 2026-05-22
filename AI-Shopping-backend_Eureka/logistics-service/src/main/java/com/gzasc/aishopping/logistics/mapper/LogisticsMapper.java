package com.gzasc.aishopping.logistics.mapper;

import com.gzasc.aishopping.logistics.model.Logistics;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LogisticsMapper {

    @Insert("INSERT INTO logistics (order_id, type, contact_id, tracking_number) " +
            "VALUES (#{orderId}, #{type}, #{contactId}, #{trackingNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertLogistics(Logistics logistics);

    @Delete("DELETE FROM logistics WHERE id = #{id}")
    int deleteLogisticsById(Integer id);

    @Select("SELECT * FROM logistics WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLogisticsById(Integer id);

    @Select("SELECT * FROM logistics")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    List<Logistics> selectAllLogistics();

    @Select("SELECT * FROM logistics WHERE tracking_number = #{trackingNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLogisticsByTrackingNumber(String trackingNumber);

    @Select("SELECT * FROM logistics WHERE order_id = #{orderId} ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    List<Logistics> selectLogisticsByOrderId(String orderId);

    @Select("SELECT * FROM logistics WHERE order_id = #{orderId} AND type = #{type} ORDER BY created_at DESC LIMIT 1")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLatestLogisticsByOrderIdAndType(@Param("orderId") String orderId, @Param("type") String type);
}
