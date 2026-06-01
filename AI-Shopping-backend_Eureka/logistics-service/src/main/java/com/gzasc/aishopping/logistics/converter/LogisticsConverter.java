package com.gzasc.aishopping.logistics.converter;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.model.Logistics;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LogisticsConverter {

    public Logistics toModel(CreateLogisticsRequest request) {
        Logistics logistics = new Logistics();
        logistics.setOrderId(request.getOrderId());
        logistics.setType(StringUtils.hasText(request.getType()) ? request.getType() : "DELIVERY");
        logistics.setContactId(request.getContactId());
        logistics.setTrackingNumber(request.getTrackingNumber());
        return logistics;
    }

    public Logistics toModel(LogisticsRequest request) {
        Logistics logistics = new Logistics();
        logistics.setOrderId(request.getOrderId());
        logistics.setType(StringUtils.hasText(request.getType()) ? request.getType() : "DELIVERY");
        logistics.setContactId(request.getContactId());
        logistics.setTrackingNumber(request.getTrackingNumber());
        return logistics;
    }

    public LogisticsResponse toResponse(Logistics logistics) {
        if (logistics == null) return null;
        return LogisticsResponse.builder()
                .id(logistics.getId())
                .orderId(logistics.getOrderId())
                .type(logistics.getType())
                .contactId(logistics.getContactId())
                .trackingNumber(logistics.getTrackingNumber())
                .createdAt(logistics.getCreatedAt())
                .build();
    }
}
