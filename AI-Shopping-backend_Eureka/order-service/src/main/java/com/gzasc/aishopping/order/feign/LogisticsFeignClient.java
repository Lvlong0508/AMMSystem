package com.gzasc.aishopping.order.feign;

import com.gzasc.aishopping.logistics.dto.LogisticsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "logistics-service")
public interface LogisticsFeignClient {

    @PostMapping("/internal/logistics/create")
    Map<String, Object> createLogistics(@RequestBody LogisticsRequest request);
}
