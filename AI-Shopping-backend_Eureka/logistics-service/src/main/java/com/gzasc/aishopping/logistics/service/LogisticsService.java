package com.gzasc.aishopping.logistics.service;

import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.dto.UpdateLogisticsRequest;
import com.gzasc.aishopping.logistics.model.Logistics;

import java.util.List;

public interface LogisticsService {

    LogisticsResponse createLogistics(CreateLogisticsRequest request);

    LogisticsResponse createLogistics(Logistics logistics);

    void deleteLogisticsById(Integer id);

    LogisticsResponse updateLogistics(UpdateLogisticsRequest request);

    LogisticsResponse getLogisticsById(Integer id);

    List<LogisticsResponse> getAllLogistics();

    LogisticsResponse getLogisticsByTrackingNumber(String trackingNumber);

    Logistics getLogisticsModelById(Integer id);
}
