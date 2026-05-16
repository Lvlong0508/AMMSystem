package com.gzasc.aishopping.common.feign.contact;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 联系人服务 Feign 客户端
 * 供其他服务调用联系人相关接口
 */
@FeignClient(name = "contact-service")
public interface ContactFeignClient {

    /**
     * 根据ID查询联系人（商家端接口）
     */
    @GetMapping("/api/seller/contact/get/{id}")
    Map<String, Object> getContactById(@PathVariable("id") int id);

    /**
     * 根据ID查询联系人（商家端接口，带用户ID）
     */
    @GetMapping("/api/seller/contact/get/{id}")
    Map<String, Object> getContactByIdWithUser(
            @PathVariable("id") int id,
            @RequestHeader("X-User-Id") String userId);
}
