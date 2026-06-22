package com.gzasc.aishopping.common.feign.contact;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 联系人服务 Feign 客户端
 * 供其他服务调用联系人相关接口
 */
@FeignClient(name = "contact-service")
public interface ContactFeignClient {

    /**
     * 根据ID查询联系人
     */
    @GetMapping("/internal/contact/{id}")
    ApiResponse<ContactDTO> getContactById(@PathVariable("id") Integer id);

    /**
     * 校验联系人是否属于该用户（用于 createOrder 等需要确认所有权的场景）
     */
    @GetMapping("/internal/contact/{contactId}/owner/{userId}")
    ApiResponse<Boolean> validateContactOwner(@PathVariable("contactId") Integer contactId,
                                               @PathVariable("userId") Long userId);
}
