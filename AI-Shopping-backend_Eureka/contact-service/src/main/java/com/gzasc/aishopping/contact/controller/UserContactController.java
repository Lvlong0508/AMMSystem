package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.dto.ContactResponse;
import com.gzasc.aishopping.contact.dto.CreateContactRequest;
import com.gzasc.aishopping.contact.dto.UpdateContactRequest;
import com.gzasc.aishopping.contact.service.UserContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/contact")
@RequiredArgsConstructor
public class UserContactController {

    private final UserContactService userContactService;

    @PostMapping("/create")
    public Map<String, Object> createContact(
            @RequestBody @Valid CreateContactRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return Map.of("code", 401, "message", "未登录");
        }
        if (bindingResult.hasErrors()) {
            return Map.of("code", 400, "message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        Integer userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return Map.of("code", 401, "message", "未登录");
        }

        Contact contact = toContact(request);
        int id = userContactService.createContact(contact, userId);
        return Map.of("code", 200, "message", "创建地址成功", "data", Map.of("id", id));
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = getUserId(userIdStr);
        if (userId == null) {
            return Map.of("code", 401, "message", "未登录");
        }

        int result = userContactService.deleteContact(id, userId);
        if (result <= 0) {
            return Map.of("code", 400, "message", "删除地址失败：地址不存在");
        }
        return Map.of("code", 200, "message", "删除地址成功");
    }

    @PutMapping("/update")
    public Map<String, Object> updateContact(
            @RequestBody @Valid UpdateContactRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return Map.of("code", 401, "message", "未登录");
        }
        if (bindingResult.hasErrors()) {
            return Map.of("code", 400, "message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        Integer userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return Map.of("code", 401, "message", "未登录");
        }

        Contact contact = toContact(request);
        int result = userContactService.updateContact(contact, userId);
        if (result <= 0) {
            return Map.of("code", 400, "message", "更新联系人失败：地址不存在");
        }
        return Map.of("code", 200, "message", "更新地址成功");
    }

    @GetMapping("/list")
    public Map<String, Object> getContacts(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = getUserId(userIdStr);
        if (userId == null) {
            return Map.of("code", 401, "message", "未登录");
        }

        List<Contact> contacts = userContactService.getContactsByUserId(userId);
        List<ContactResponse> data = contacts.stream()
                .map(ContactResponse::fromContact)
                .collect(Collectors.toList());
        return Map.of("code", 200, "message", "查询成功", "data", Map.of("contacts", data, "total", data.size()));
    }

    @PutMapping("/set-default/{id}")
    public Map<String, Object> setDefaultContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = getUserId(userIdStr);
        if (userId == null) {
            return Map.of("code", 401, "message", "未登录");
        }

        int result = userContactService.setDefaultContact(id, userId);
        if (result <= 0) {
            return Map.of("code", 400, "message", "设置失败：地址不存在");
        }
        return Map.of("code", 200, "message", "设置成功");
    }

    private Integer getUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Contact toContact(CreateContactRequest request) {
        Contact contact = new Contact();
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setAddress(request.getAddress());
        return contact;
    }

    private Contact toContact(UpdateContactRequest request) {
        Contact contact = toContact(request);
        contact.setId(request.getId());
        return contact;
    }
}