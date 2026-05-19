package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.ContactService;
import com.gzasc.aishopping.contact.service.impl.ContactException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/create")
    public Map<String, Object> createContact(
            @RequestBody @Valid Contact contact,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("未登录（错误代码：Co-000）");
        }
        int id = contactService.createContact(contact, userId);
        return Map.of("code", 200, "message", "创建联系人成功", "data", Map.of("id", id));
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("删除联系人错误：未登录（错误代码：Co-000）");
        }
        int result = contactService.deleteContact(id, userId);
        if (result <= 0) {
            throw new ContactException("删除联系人失败：联系人不存在或无权限（错误代码：Co-005）");
        }
        return Map.of("code", 200, "message", "删除联系人成功");
    }

    @PutMapping("/update")
    public Map<String, Object> updateContact(
            @RequestBody @Valid Contact contact,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("更新联系人错误：未登录（错误代码：Co-000）");
        }
        if (contact.getId() <= 0) {
            throw new ContactException("更新联系人错误：ID无效（错误代码：Co-006）");
        }
        int result = contactService.updateContact(contact, userId);
        if (result <= 0) {
            throw new ContactException("更新联系人失败：联系人不存在或无权限（错误代码：Co-010）");
        }
        return Map.of("code", 200, "message", "更新联系人成功");
    }

    @GetMapping("/get/{id}")
    public Map<String, Object> getContactById(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("查询联系人错误：未登录（错误代码：Co-000）");
        }
        Contact contact = contactService.getContactById(id, userId);
        if (contact == null) {
            throw new ContactException("查询失败：联系人不存在（错误代码：Co-011）");
        }
        return Map.of("code", 200, "message", "查询成功", "data", contact);
    }

    @GetMapping("/list")
    public Map<String, Object> getContacts(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("查询联系人错误：未登录（错误代码：Co-000）");
        }
        List<Contact> contacts = contactService.getContactsByUserId(userId);
        return Map.of("code", 200, "message", "查询成功", "data", contacts, "total", contacts.size());
    }

    @GetMapping("/search/name")
    public Map<String, Object> getContactsByName(@RequestParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ContactException("查询错误：姓名为空（错误代码：Co-012）");
        }
        List<Contact> contacts = contactService.getContactsByName(name);
        return Map.of("code", 200, "message", "查询成功", "data", contacts, "total", contacts.size());
    }

    @GetMapping("/search/phone")
    public Map<String, Object> getContactByPhone(@RequestParam("phone") String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ContactException("查询错误：电话为空（错误代码：Co-013）");
        }
        Contact contact = contactService.getContactByPhone(phone);
        if (contact == null) {
            throw new ContactException("查询失败：联系人不存在（错误代码：Co-014）");
        }
        return Map.of("code", 200, "message", "查询成功", "data", contact);
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