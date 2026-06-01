package com.gzasc.aishopping.logistics.service.impl;

import com.gzasc.aishopping.logistics.converter.LogisticsConverter;
import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.exception.LogisticsException;
import com.gzasc.aishopping.logistics.mapper.LogisticsMapper;
import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    private final LogisticsMapper logisticsMapper;
    private final LogisticsConverter logisticsConverter;

    @Override
    @Transactional
    public LogisticsResponse createLogistics(CreateLogisticsRequest request) {
        Logistics logistics = logisticsConverter.toModel(request);
        return doCreateLogistics(logistics);
    }

    @Override
    @Transactional
    public LogisticsResponse createLogistics(Logistics logistics) {
        return doCreateLogistics(logistics);
    }

    private LogisticsResponse doCreateLogistics(Logistics logistics) {
        int result = logisticsMapper.insertLogistics(logistics);
        if (result <= 0) {
            throw new LogisticsException("创建物流信息失败");
        }
        return logisticsConverter.toResponse(logistics);
    }

    @Override
    @Transactional
    public void deleteLogisticsById(Integer id) {
        Logistics exists = logisticsMapper.selectLogisticsById(id);
        if (exists == null) {
            throw new LogisticsException("物流信息不存在");
        }
        logisticsMapper.deleteLogisticsById(id);
    }

    @Override
    public LogisticsResponse getLogisticsById(Integer id) {
        Logistics logistics = logisticsMapper.selectLogisticsById(id);
        if (logistics == null) {
            throw new LogisticsException("物流信息不存在");
        }
        return logisticsConverter.toResponse(logistics);
    }

    @Override
    public List<LogisticsResponse> getAllLogistics() {
        return logisticsMapper.selectAllLogistics().stream()
                .map(logisticsConverter::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LogisticsResponse getLogisticsByTrackingNumber(String trackingNumber) {
        Logistics logistics = logisticsMapper.selectLogisticsByTrackingNumber(trackingNumber);
        if (logistics == null) {
            throw new LogisticsException("物流信息不存在");
        }
        return logisticsConverter.toResponse(logistics);
    }

    @Override
    public List<LogisticsResponse> getLogisticsByOrderId(String orderId) {
        return logisticsMapper.selectLogisticsByOrderId(orderId).stream()
                .map(logisticsConverter::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LogisticsResponse getLatestLogistics(String orderId, String type) {
        Logistics logistics = logisticsMapper.selectLatestLogisticsByOrderIdAndType(orderId, type);
        if (logistics == null) {
            throw new LogisticsException("物流信息不存在");
        }
        return logisticsConverter.toResponse(logistics);
    }
}
