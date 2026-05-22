package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.logistics.converter.LogisticsConverter;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/get/{id}")
    public ApiResponse<LogisticsResponse> getLogisticsById(@PathVariable("id") Integer id) {
        LogisticsResponse logistics = logisticsService.getLogisticsById(id);
        return ApiResponse.success("查询成功", logistics);
    }

    @PutMapping("/close/{id}")
    public ApiResponse<Void> closeLogistics(@PathVariable("id") Integer id) {
        logisticsService.deleteLogisticsById(id);
        return ApiResponse.success("物流信息已关闭", null);
    }
}
