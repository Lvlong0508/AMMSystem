package com.gzasc.aishopping.common.feign.contact;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 联系人服务 Feign 客户端
 * 供其他服务调用联系人相关接口
 */
@FeignClient(name = "contact-service")
public interface ContactFeignClient {

    /**
     * 根据ID查询联系人（用户端接口）
     */
    @GetMapping("/api/user/contact/{id}")
    Map<String, Object> getContactById(@PathVariable("id") int id);
}
