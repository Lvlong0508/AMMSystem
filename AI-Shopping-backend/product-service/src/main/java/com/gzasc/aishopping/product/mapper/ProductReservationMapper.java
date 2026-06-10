package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.ProductReservation;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

@Mapper
public interface ProductReservationMapper {

    @Insert("INSERT INTO product_reservations (product_id, order_id, quantity, status, created_at, expired_at) " +
            "VALUES (#{productId}, #{orderId}, #{quantity}, #{status}, #{createdAt}, #{expiredAt})")
    int insertReservation(ProductReservation reservation);

    @Select("SELECT * FROM product_reservations WHERE order_id = #{orderId}")
    ProductReservation selectByOrderId(@Param("orderId") String orderId);

    @Update("UPDATE product_reservations SET status = 'CONFIRMED' WHERE order_id = #{orderId} AND status = 'RESERVED'")
    int confirmReservation(@Param("orderId") String orderId);

    @Update("UPDATE product_reservations SET status = 'RELEASED' WHERE order_id = #{orderId} AND status = 'RESERVED'")
    int releaseReservation(@Param("orderId") String orderId);

    @Select("SELECT * FROM product_reservations WHERE status = 'RESERVED' AND expired_at <= #{now}")
    List<ProductReservation> selectExpiredReservations(@Param("now") Date now);


    @Select("SELECT stock FROM products WHERE id = #{productId} FOR UPDATE")
    Integer selectProductStockForUpdate(@Param("productId") Long productId);

    @Select("SELECT COALESCE(SUM(quantity), 0) FROM product_reservations " +
            "WHERE product_id = #{productId} AND status = 'RESERVED' FOR UPDATE")
    int sumReservedQty(@Param("productId") Long productId);
}
