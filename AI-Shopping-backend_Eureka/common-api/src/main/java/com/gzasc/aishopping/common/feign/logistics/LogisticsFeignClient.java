package com.gzasc.aishopping.common.feign.logistics;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 物流服务 Feign 客户端
 * 供其他服务调用物流相关接口
 */
@FeignClient(name = "logistics-service")
public interface LogisticsFeignClient {

    /**
     * 创建物流记录
     */
    @PostMapping("/internal/logistics/create")
    Map<String, Object> createLogistics(@RequestBody LogisticsRequest request);

    /**
     * 关闭/取消物流记录（补偿用）
     */
    @PutMapping("/internal/logistics/close/{id}")
    Map<String, Object> closeLogistics(@PathVariable("id") Integer id);

    /**
     * 根据ID获取物流信息
     */
    @GetMapping("/internal/logistics/get/{id}")
    Map<String, Object> getLogisticsById(@PathVariable("id") Integer id);
}
