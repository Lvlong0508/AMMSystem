package com.gzasc.aishopping.logistics.service;

import com.gzasc.aishopping.logistics.model.Logistics;

import java.util.List;

public interface LogisticsService {

    int createLogistics(Logistics logistics);

    int deleteLogisticsById(Integer id);

    int updateLogistics(Logistics logistics);

    Logistics getLogisticsById(Integer id);

    List<Logistics> getAllLogistics();

    Logistics getLogisticsByTrackingNumber(String trackingNumber);
}
