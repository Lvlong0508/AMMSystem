package com.gzasc.aishopping.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "logistics-service")
public interface LogisticsFeignClient {

    @PostMapping("/internal/logistics/create")
    Map<String, Object> createLogistics(@RequestBody LogisticsRequest request);

    class LogisticsRequest {
        private Integer contactId;
        private String trackingNumber;
        private String shippingDate;

        public LogisticsRequest() {}

        public LogisticsRequest(Integer contactId, String trackingNumber, String shippingDate) {
            this.contactId = contactId;
            this.trackingNumber = trackingNumber;
            this.shippingDate = shippingDate;
        }

        public Integer getContactId() { return contactId; }
        public void setContactId(Integer contactId) { this.contactId = contactId; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public String getShippingDate() { return shippingDate; }
        public void setShippingDate(String shippingDate) { this.shippingDate = shippingDate; }
    }
}
