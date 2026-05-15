package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/contact")
@RequiredArgsConstructor
public class ContactSellerController {

    private final ContactService contactService;

    @GetMapping("/list")
    public Map<String, Object> getAllContacts(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            return Map.of("message", "查询联系人错误：未登录");
        }

        try {
            List<Contact> contacts = contactService.getContactsByUserId(userId);
            return Map.of("message", "查询成功", "data", contacts, "total", contacts.size());
        } catch (Exception e) {
            return Map.of("message", "查询联系人列表错误：" + e.getMessage());
        }
    }

    @GetMapping("/get/{id}")
    public Map<String, Object> getContactById(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            return Map.of("message", "查询联系人错误：未登录");
        }

        try {
            Contact contact = contactService.getContactById(id, userId);
            if (contact != null) {
                return Map.of("message", "查询成功", "data", contact);
            } else {
                return Map.of("message", "查询失败：联系人不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "查询联系人错误：" + e.getMessage());
        }
    }

    private Integer parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}