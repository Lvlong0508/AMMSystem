package com.gzasc.aishopping.logistics.mapper;

import com.gzasc.aishopping.logistics.model.Logistics;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LogisticsMapper {

    @Insert("INSERT INTO logistics (contact_id, shipping_date, tracking_number) " +
            "VALUES (#{contactId}, #{shippingDate}, #{trackingNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertLogistics(Logistics logistics);

    @Delete("DELETE FROM logistics WHERE id = #{id}")
    int deleteLogisticsById(Integer id);

    @Update("UPDATE logistics SET contact_id = #{contactId}, shipping_date = #{shippingDate}, " +
            "tracking_number = #{trackingNumber} WHERE id = #{id}")
    int updateLogistics(Logistics logistics);

    @Select("SELECT * FROM logistics WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "shippingDate", column = "shipping_date"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLogisticsById(Integer id);

    @Select("SELECT * FROM logistics")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "shippingDate", column = "shipping_date"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    List<Logistics> selectAllLogistics();

    @Select("SELECT * FROM logistics WHERE tracking_number = #{trackingNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "shippingDate", column = "shipping_date"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLogisticsByTrackingNumber(String trackingNumber);
}
