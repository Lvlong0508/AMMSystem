package com.gzasc.aishopping.contact.controller.internal;

import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/contact")
@RequiredArgsConstructor
public class InternalContactController {

    private final ContactService contactService;

    @GetMapping("/{id}")
    public Map<String, Object> getContactById(@PathVariable("id") int id) {
        try {
            Contact contact = contactService.getContactByIdNoAuth(id);
            if (contact != null) {
                return Map.of("success", true, "data", contact);
            } else {
                return Map.of("success", false, "message", "联系人不存在");
            }
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询失败：" + e.getMessage());
        }
    }
}