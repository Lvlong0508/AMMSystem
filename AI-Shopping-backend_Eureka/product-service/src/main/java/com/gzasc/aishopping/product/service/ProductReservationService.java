package com.gzasc.aishopping.product.service;

public interface ProductReservationService {

    void reserve(String orderId, String productId, int quantity);

    void confirm(String orderId);

    void release(String orderId);

    void releaseExpiredReservations();
}
