package com.gzasc.aishopping.logistics.converter;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.dto.UpdateLogisticsRequest;
import com.gzasc.aishopping.logistics.model.Logistics;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class LogisticsConverter {

    public Logistics toModel(CreateLogisticsRequest request) {
        Logistics logistics = new Logistics();
        logistics.setContactId(request.getContactId());
        logistics.setTrackingNumber(request.getTrackingNumber());
        if (request.getShippingDate() != null) {
            logistics.setShippingDate(Timestamp.valueOf(request.getShippingDate().replace('T', ' ').substring(0, 19)));
        }
        return logistics;
    }

    public Logistics toModel(UpdateLogisticsRequest request) {
        Logistics logistics = new Logistics();
        logistics.setId(request.getId());
        logistics.setContactId(request.getContactId());
        logistics.setTrackingNumber(request.getTrackingNumber());
        if (request.getShippingDate() != null) {
            logistics.setShippingDate(Timestamp.valueOf(request.getShippingDate().replace('T', ' ').substring(0, 19)));
        }
        return logistics;
    }

    public Logistics toModel(LogisticsRequest request) {
        Logistics logistics = new Logistics();
        logistics.setContactId(request.getContactId());
        logistics.setTrackingNumber(request.getTrackingNumber());
        if (request.getShippingDate() != null && !request.getShippingDate().isEmpty()) {
            String dateStr = request.getShippingDate().replace('T', ' ');
            logistics.setShippingDate(Timestamp.valueOf(dateStr.length() == 16 ? dateStr + ":00" : dateStr));
        }
        return logistics;
    }

    public LogisticsResponse toResponse(Logistics logistics) {
        if (logistics == null) return null;
        return LogisticsResponse.builder()
                .id(logistics.getId())
                .contactId(logistics.getContactId())
                .trackingNumber(logistics.getTrackingNumber())
                .shippingDate(logistics.getShippingDate())
                .build();
    }
}
