package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/logistics")
@RequiredArgsConstructor
public class LogisticsController {

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

    @GetMapping("/get/{id}")
    public Map<String, Object> getLogisticsById(@PathVariable("id") Integer id) {
        try {
            Logistics logistics = logisticsService.getLogisticsById(id);
            if (logistics != null) {
                return Map.of("message", "查询成功", "data", logistics);
            } else {
                return Map.of("message", "查询失败：物流信息不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "查询物流信息错误：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Map<String, Object> getAllLogistics() {
        try {
            List<Logistics> logistics = logisticsService.getAllLogistics();
            return Map.of("message", "查询成功", "data", logistics, "total", logistics.size());
        } catch (Exception e) {
            return Map.of("message", "查询物流信息错误：" + e.getMessage());
        }
    }

    @GetMapping("/search/tracking")
    public Map<String, Object> getLogisticsByTrackingNumber(@RequestParam("trackingNumber") String trackingNumber) {
        try {
            Logistics logistics = logisticsService.getLogisticsByTrackingNumber(trackingNumber);
            if (logistics != null) {
                return Map.of("message", "查询成功", "data", logistics);
            } else {
                return Map.of("message", "查询失败：物流信息不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "查询物流信息错误：" + e.getMessage());
        }
    }

    @PutMapping("/update")
    public Map<String, String> updateLogistics(@RequestBody Logistics logistics) {
        try {
            int result = logisticsService.updateLogistics(logistics);
            if (result > 0) {
                return Map.of("message", "更新物流信息成功");
            } else {
                return Map.of("message", "更新物流信息失败：物流信息不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "更新物流信息错误：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, String> deleteLogistics(@PathVariable("id") Integer id) {
        try {
            int result = logisticsService.deleteLogisticsById(id);
            if (result > 0) {
                return Map.of("message", "删除物流信息成功");
            } else {
                return Map.of("message", "删除物流信息失败：物流信息不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "删除物流信息错误：" + e.getMessage());
        }
    }
}
