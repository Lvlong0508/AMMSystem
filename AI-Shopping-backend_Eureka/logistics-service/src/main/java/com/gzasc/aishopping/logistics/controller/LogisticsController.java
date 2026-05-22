package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.dto.UpdateLogisticsRequest;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    @PostMapping("/create")
    public ApiResponse<LogisticsResponse> createLogistics(@RequestBody @Valid CreateLogisticsRequest request) {
        LogisticsResponse result = logisticsService.createLogistics(request);
        return ApiResponse.success("创建物流信息成功", result);
    }

    @GetMapping("/get/{id}")
    public ApiResponse<LogisticsResponse> getLogisticsById(@PathVariable("id") Integer id) {
        LogisticsResponse logistics = logisticsService.getLogisticsById(id);
        return ApiResponse.success("查询成功", logistics);
    }

    @GetMapping("/list")
    public ApiResponse<List<LogisticsResponse>> getAllLogistics() {
        List<LogisticsResponse> logistics = logisticsService.getAllLogistics();
        return ApiResponse.success("查询成功", logistics);
    }

    @GetMapping("/search/tracking")
    public ApiResponse<LogisticsResponse> getLogisticsByTrackingNumber(@RequestParam("trackingNumber") String trackingNumber) {
        LogisticsResponse logistics = logisticsService.getLogisticsByTrackingNumber(trackingNumber);
        return ApiResponse.success("查询成功", logistics);
    }

    @PutMapping("/update")
    public ApiResponse<LogisticsResponse> updateLogistics(@RequestBody @Valid UpdateLogisticsRequest request) {
        LogisticsResponse result = logisticsService.updateLogistics(request);
        return ApiResponse.success("更新物流信息成功", result);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteLogistics(@PathVariable("id") Integer id) {
        logisticsService.deleteLogisticsById(id);
        return ApiResponse.success("删除物流信息成功", null);
    }
}
