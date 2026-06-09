package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductReservationMapper;
import com.gzasc.aishopping.product.mapper.ProductStockMapper;
import com.gzasc.aishopping.product.model.ProductReservation;
import com.gzasc.aishopping.product.service.ProductReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReservationServiceImpl implements ProductReservationService {

    private final ProductReservationMapper mapper;
    private final ProductStockMapper productStockMapper;

    @Value("")
    private int paymentTimeoutMinutes;

    @Override
    @Transactional
    public void reserve(String orderId, String productId, int quantity) {
        Long pid = Long.valueOf(productId);
        Integer stock = mapper.selectProductStockForUpdate(pid);
        if (stock == null) {
            throw new ProductException(400, "商品不存在");
        }
        int alreadyReserved = mapper.sumReservedQty(pid);
        if (stock - alreadyReserved < quantity) {
            throw new ProductException(409, "商品库存不足");
        }
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.MINUTE, paymentTimeoutMinutes);
        Date expiredAt = cal.getTime();
        ProductReservation reservation = new ProductReservation();
        reservation.setProductId(productId);
        reservation.setOrderId(orderId);
        reservation.setQuantity(quantity);
        reservation.setStatus(ProductReservation.RESERVED);
        reservation.setCreatedAt(now);
        reservation.setExpiredAt(expiredAt);
        mapper.insertReservation(reservation);
        log.info("预占库存成功 orderId={}, productId={}, quantity={}", orderId, productId, quantity);
    }

    @Override
    @Transactional
    public void confirm(String orderId) {
        ProductReservation reservation = mapper.selectByOrderId(orderId);
        if (reservation == null) throw new ProductException("预占记录不存在");
        if (!ProductReservation.RESERVED.equals(reservation.getStatus())) throw new ProductException("预占状态已变更");
        int rows = mapper.confirmReservation(orderId);
        if (rows <= 0) throw new ProductException("确认预占失败");
        rows = productStockMapper.deductStock(Long.valueOf(reservation.getProductId()), reservation.getQuantity());
        if (rows <= 0) throw new ProductException("扣减库存失败");
        log.info("确认预占成功 orderId={}, productId={}, quantity={}", orderId, reservation.getProductId(), reservation.getQuantity());
    }

    @Override
    @Transactional
    public void release(String orderId) {
        ProductReservation reservation = mapper.selectByOrderId(orderId);
        if (reservation == null) return;
        if (ProductReservation.RELEASED.equals(reservation.getStatus())) return;
        if (!ProductReservation.RESERVED.equals(reservation.getStatus())) throw new ProductException("预占状态不允许释放");
        mapper.releaseReservation(orderId);
        log.info("释放预占成功 orderId={}", orderId);
    }

    @Override
    public void releaseExpiredReservations() {
        List<ProductReservation> expiredList = mapper.selectExpiredReservations(new Date());
        for (ProductReservation r : expiredList) {
            try { release(r.getOrderId()); }
            catch (Exception e) { log.warn("释放过期预占失败 orderId={}", r.getOrderId(), e); }
        }
    }
}
