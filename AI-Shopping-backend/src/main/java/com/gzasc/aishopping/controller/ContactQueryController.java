package com.gzasc.aishopping.controller;

import com.gzasc.aishopping.model.Contact;
import com.gzasc.aishopping.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactQueryController {

    private final ContactService contactService;

    /**
     * 根据ID查询联系人
     */
    @GetMapping("/get/{id}")
    public Map<String, Object> getContactById(@PathVariable int id) {
        try {
            Contact contact = contactService.getContactById(id);
            if (contact != null) {
                return Map.of("message", "查询成功", "data", contact);
            } else {
                return Map.of("message", "查询失败：联系人不存在（错误代码：Co-011）");
            }
        } catch (Exception e) {
            return Map.of("message", "查询联系人错误：" + e.getMessage());
        }
    }

    /**
     * 查询所有联系人
     */
    @GetMapping("/list")
    public Map<String, Object> getAllContacts() {
        try {
            List<Contact> contacts = contactService.getAllContacts();
            return Map.of("message", "查询成功", "data", contacts, "total", contacts.size());
        } catch (Exception e) {
            return Map.of("message", "查询联系人列表错误：" + e.getMessage());
        }
    }

    /**
     * 根据姓名查询联系人
     */
    @GetMapping("/search/name")
    public Map<String, Object> getContactsByName(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return Map.of("message", "查询错误：姓名为空（错误代码：Co-012）");
        }
        try {
            List<Contact> contacts = contactService.getContactsByName(name);
            return Map.of("message", "查询成功", "data", contacts, "total", contacts.size());
        } catch (Exception e) {
            return Map.of("message", "查询联系人错误：" + e.getMessage());
        }
    }

    /**
     * 根据电话查询联系人
     */
    @GetMapping("/search/phone")
    public Map<String, Object> getContactByPhone(@RequestParam String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Map.of("message", "查询错误：电话为空（错误代码：Co-013）");
        }
        try {
            Contact contact = contactService.getContactByPhone(phone);
            if (contact != null) {
                return Map.of("message", "查询成功", "data", contact);
            } else {
                return Map.of("message", "查询失败：联系人不存在（错误代码：Co-014）");
            }
        } catch (Exception e) {
            return Map.of("message", "查询联系人错误：" + e.getMessage());
        }
    }
}
