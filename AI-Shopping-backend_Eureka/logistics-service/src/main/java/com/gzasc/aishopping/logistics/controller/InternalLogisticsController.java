package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

import java.sql.Timestamp;
import java.util.Map;

@RestController
@RequestMapping("/internal/logistics")
@RequiredArgsConstructor
public class InternalLogisticsController {

    private final LogisticsService logisticsService;

    @PostMapping("/create")
    public Map<String, Object> createLogistics(@RequestBody LogisticsRequest request) {
        try {
            Logistics logistics = new Logistics();
            logistics.setContactId(request.getContactId());
            logistics.setTrackingNumber(request.getTrackingNumber());
            if (request.getShippingDate() != null) {
                logistics.setShippingDate(Timestamp.valueOf(request.getShippingDate()));
            }

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

    /**
     * 关闭/取消物流记录（补偿用）
     */
    @PutMapping("/close/{id}")
    public Map<String, Object> closeLogistics(@PathVariable("id") Integer id) {
        try {
            if (id == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("message", "关闭物流失败：ID不能为空");
                result.put("success", false);
                return result;
            }

            Logistics logistics = logisticsService.getLogisticsById(id);
            if (logistics == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("message", "关闭物流失败：记录不存在");
                result.put("success", false);
                return result;
            }

            int deleteResult = logisticsService.deleteLogisticsById(id);
            Map<String, Object> result = new HashMap<>();
            if (deleteResult > 0) {
                result.put("message", "关闭物流记录成功");
                result.put("success", true);
            } else {
                result.put("message", "关闭物流记录失败");
                result.put("success", false);
            }
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "关闭物流记录错误：" + e.getMessage());
            result.put("success", false);
            return result;
        }
    }
}
