package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Map;

@RestController
@RequestMapping("/internal/logistics")
@RequiredArgsConstructor
public class InternalLogisticsController {

    private final LogisticsService logisticsService;

    @PostMapping("/create")
    public Map<String, Object> createLogistics(@RequestBody Logistics logistics) {
        try {
            int result = logisticsService.createLogistics(logistics);
            if (result > 0) {
                return Map.of("message", "创建物流信息成功", "data", logistics);
            } else {
                return Map.of("message", "创建物流信息失败");
            }
        } catch (Exception e) {
            return Map.of("message", "创建物流信息错误：" + e.getMessage());
        }
    }
}
