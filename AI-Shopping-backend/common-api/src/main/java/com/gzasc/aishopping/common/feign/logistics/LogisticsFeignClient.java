package com.gzasc.aishopping.common.feign.logistics;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "logistics-service")
public interface LogisticsFeignClient {

    @PostMapping("/internal/logistics/create")
    ApiResponse<Map<String, Object>> createLogistics(@RequestBody LogisticsRequest request);

    @GetMapping("/internal/logistics/order/{orderId}")
    ApiResponse<List<Map<String, Object>>> getLogisticsByOrder(@PathVariable("orderId") String orderId);

    @GetMapping("/internal/logistics/order/{orderId}/latest")
    ApiResponse<Map<String, Object>> getLatestLogistics(
            @PathVariable("orderId") String orderId,
            @RequestParam("type") String type);
}
