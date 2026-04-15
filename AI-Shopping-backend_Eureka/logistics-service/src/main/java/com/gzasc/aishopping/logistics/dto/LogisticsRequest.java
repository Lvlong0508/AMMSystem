package com.gzasc.aishopping.logistics.dto;

import lombok.Data;

@Data
public class LogisticsRequest {
        private Integer contactId;
        private String trackingNumber;
        private String shippingDate;

        public LogisticsRequest(Integer contactId, String trackingNumber, String shippingDate) {
            this.contactId = contactId;
            this.trackingNumber = trackingNumber;
            this.shippingDate = shippingDate;
        }
}
