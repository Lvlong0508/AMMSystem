package com.gzasc.aishopping.controller;

import com.gzasc.aishopping.mapper.LogisticsMapper;
import com.gzasc.aishopping.model.child.Logistics;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsMapper logisticsMapper;

    // 创建物流信息
    @PostMapping("/create")
    public Map<String, Object> createLogistics(@RequestBody Logistics logistics) {
        try {
            int result = logisticsMapper.insertLogistics(logistics);
            if (result > 0) {
                return Map.of("message", "创建物流信息成功", "data", logistics);
            } else {
                return Map.of("message", "创建物流信息失败");
            }
        } catch (Exception e) {
            return Map.of("message", "创建物流信息错误：" + e.getMessage());
        }
    }

    // 根据ID查询物流信息
    @GetMapping("/get/{id}")
    public Map<String, Object> getLogisticsById(@PathVariable("id") Integer id) {
        try {
            Logistics logistics = logisticsMapper.selectLogisticsById(id);
            if (logistics != null) {
                return Map.of("message", "查询成功", "data", logistics);
            } else {
                return Map.of("message", "查询失败：物流信息不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "查询物流信息错误：" + e.getMessage());
        }
    }

    // 查询所有物流信息
    @GetMapping("/list")
    public Map<String, Object> getAllLogistics() {
        try {
            List<Logistics> logistics = logisticsMapper.selectAllLogistics();
            return Map.of("message", "查询成功", "data", logistics, "total", logistics.size());
        } catch (Exception e) {
            return Map.of("message", "查询物流信息错误：" + e.getMessage());
        }
    }

    // 根据快递单号查询物流信息
    @GetMapping("/search/tracking")
    public Map<String, Object> getLogisticsByTrackingNumber(@RequestParam("trackingNumber") String trackingNumber) {
        try {
            Logistics logistics = logisticsMapper.selectLogisticsByTrackingNumber(trackingNumber);
            if (logistics != null) {
                return Map.of("message", "查询成功", "data", logistics);
            } else {
                return Map.of("message", "查询失败：物流信息不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "查询物流信息错误：" + e.getMessage());
        }
    }

    // 更新物流信息
    @PutMapping("/update")
    public Map<String, String> updateLogistics(@RequestBody Logistics logistics) {
        try {
            int result = logisticsMapper.updateLogistics(logistics);
            if (result > 0) {
                return Map.of("message", "更新物流信息成功");
            } else {
                return Map.of("message", "更新物流信息失败：物流信息不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "更新物流信息错误：" + e.getMessage());
        }
    }

    // 删除物流信息
    @DeleteMapping("/delete/{id}")
    public Map<String, String> deleteLogistics(@PathVariable("id") Integer id) {
        try {
            int result = logisticsMapper.deleteLogisticsById(id);
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
