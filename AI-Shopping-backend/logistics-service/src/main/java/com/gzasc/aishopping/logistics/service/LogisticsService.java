package com.gzasc.aishopping.logistics.service;

import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.model.Logistics;

import java.util.List;

public interface LogisticsService {

    LogisticsResponse createLogistics(CreateLogisticsRequest request);

    LogisticsResponse createLogistics(Logistics logistics);

    void deleteLogisticsById(Integer id);

    LogisticsResponse getLogisticsById(Integer id);

    List<LogisticsResponse> getAllLogistics();

    LogisticsResponse getLogisticsByTrackingNumber(String trackingNumber);

    List<LogisticsResponse> getLogisticsByOrderId(String orderId);

    LogisticsResponse getLatestLogistics(String orderId, String type);
}
