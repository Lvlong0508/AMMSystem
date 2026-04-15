package com.gzasc.aishopping.logistics.service.impl;

import com.gzasc.aishopping.logistics.mapper.LogisticsMapper;
import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    private final LogisticsMapper logisticsMapper;

    @Override
    public int createLogistics(Logistics logistics) {
        return logisticsMapper.insertLogistics(logistics);
    }

    @Override
    public int deleteLogisticsById(Integer id) {
        return logisticsMapper.deleteLogisticsById(id);
    }

    @Override
    public int updateLogistics(Logistics logistics) {
        return logisticsMapper.updateLogistics(logistics);
    }

    @Override
    public Logistics getLogisticsById(Integer id) {
        return logisticsMapper.selectLogisticsById(id);
    }

    @Override
    public List<Logistics> getAllLogistics() {
        return logisticsMapper.selectAllLogistics();
    }

    @Override
    public Logistics getLogisticsByTrackingNumber(String trackingNumber) {
        return logisticsMapper.selectLogisticsByTrackingNumber(trackingNumber);
    }
}
