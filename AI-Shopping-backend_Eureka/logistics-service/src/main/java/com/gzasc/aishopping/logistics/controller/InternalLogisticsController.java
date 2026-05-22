package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.logistics.converter.LogisticsConverter;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/logistics")
@RequiredArgsConstructor
public class InternalLogisticsController {

    private final LogisticsService logisticsService;
    private final LogisticsConverter logisticsConverter;

    @PostMapping("/create")
    public ApiResponse<LogisticsResponse> createLogistics(@RequestBody LogisticsRequest request) {
        Logistics logistics = logisticsConverter.toModel(request);
        LogisticsResponse result = logisticsService.createLogistics(logistics);
        return ApiResponse.success("创建物流信息成功", result);
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<LogisticsResponse>> getLogisticsByOrder(@PathVariable("orderId") String orderId) {
        List<LogisticsResponse> logistics = logisticsService.getLogisticsByOrderId(orderId);
        return ApiResponse.success("查询成功", logistics);
    }

    @GetMapping("/order/{orderId}/latest")
    public ApiResponse<LogisticsResponse> getLatestLogistics(
            @PathVariable("orderId") String orderId,
            @RequestParam("type") String type) {
        LogisticsResponse logistics = logisticsService.getLatestLogistics(orderId, type);
        return ApiResponse.success("查询成功", logistics);
    }
}
