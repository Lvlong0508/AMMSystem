package com.gzasc.aishopping.chat.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "contact-service")
public interface ContactFeignClient {

    @GetMapping("/api/contact/get/{id}")
    Map<String, Object> getContactById(@PathVariable("id") int id);
}
