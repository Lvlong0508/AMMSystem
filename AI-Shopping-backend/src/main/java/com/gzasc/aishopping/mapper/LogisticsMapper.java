package com.gzasc.aishopping.mapper;

import com.gzasc.aishopping.model.Logistics;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LogisticsMapper {

    /**
     * 插入物流信息
     */
    @Insert("INSERT INTO logistics (contact_id, shipping_date, tracking_number) " +
            "VALUES (#{contactId}, #{shippingDate}, #{trackingNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertLogistics(Logistics logistics);

    /**
     * 根据物流ID删除物流信息
     */
    @Delete("DELETE FROM logistics WHERE id = #{id}")
    int deleteLogisticsById(Integer id);

    /**
     * 更新物流信息
     */
    @Update("UPDATE logistics SET contact_id = #{contactId}, shipping_date = #{shippingDate}, " +
            "tracking_number = #{trackingNumber} WHERE id = #{id}")
    int updateLogistics(Logistics logistics);

    /**
     * 根据物流ID查询物流信息
     */
    @Select("SELECT * FROM logistics WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "shippingDate", column = "shipping_date"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLogisticsById(Integer id);

    /**
     * 查询所有物流信息
     */
    @Select("SELECT * FROM logistics")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "shippingDate", column = "shipping_date"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    List<Logistics> selectAllLogistics();

    /**
     * 根据快递单号查询物流信息
     */
    @Select("SELECT * FROM logistics WHERE tracking_number = #{trackingNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "shippingDate", column = "shipping_date"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLogisticsByTrackingNumber(String trackingNumber);
}
