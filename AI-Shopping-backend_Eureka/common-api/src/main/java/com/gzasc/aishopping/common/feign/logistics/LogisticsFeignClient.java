package com.gzasc.aishopping.common.feign.logistics;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
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
}
