package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductReservationMapper;
import com.gzasc.aishopping.product.model.ProductReservation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReservationService {

    private static final Logger log = LoggerFactory.getLogger(ProductReservationService.class);

    private final ProductReservationMapper mapper;

    @Transactional
    public void reserve(String orderId, String productId, int quantity) {
        int stock = mapper.selectProductStockForUpdate(productId);
        int alreadyReserved = mapper.sumReservedQty(productId);
        if (stock - alreadyReserved < quantity) {
            throw new ProductException("商品库存不足");
        }

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.MINUTE, 30);
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

    @Transactional
    public void confirm(String orderId) {
        ProductReservation reservation = mapper.selectByOrderId(orderId);
        if (reservation == null) {
            throw new ProductException("预占记录不存在");
        }
        if (!ProductReservation.RESERVED.equals(reservation.getStatus())) {
            throw new ProductException("预占状态已变更，无法确认");
        }

        int rows = mapper.confirmReservation(orderId);
        if (rows <= 0) {
            throw new ProductException("确认预占失败");
        }

        rows = mapper.deductProductStock(reservation.getProductId(), reservation.getQuantity());
        if (rows <= 0) {
            throw new ProductException("扣减库存失败");
        }
        log.info("确认预占成功 orderId={}, productId={}, quantity={}", orderId, reservation.getProductId(), reservation.getQuantity());
    }

    @Transactional
    public void release(String orderId) {
        ProductReservation reservation = mapper.selectByOrderId(orderId);
        if (reservation == null) {
            return;
        }
        if (ProductReservation.RELEASED.equals(reservation.getStatus())) {
            return;
        }
        if (!ProductReservation.RESERVED.equals(reservation.getStatus())) {
            throw new ProductException("预占状态不允许释放");
        }
        mapper.releaseReservation(orderId);
        log.info("释放预占成功 orderId={}", orderId);
    }

    @Transactional
    public void releaseExpiredReservations() {
        List<ProductReservation> expiredList = mapper.selectExpiredReservations(new Date());
        for (ProductReservation r : expiredList) {
            release(r.getOrderId());
        }
    }
}
