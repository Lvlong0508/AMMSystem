package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.model.dto.ApiResponse;
import com.gzasc.aishopping.contact.model.dto.ContactResponse;
import com.gzasc.aishopping.contact.model.dto.CreateContactRequest;
import com.gzasc.aishopping.contact.model.dto.UpdateContactRequest;
import com.gzasc.aishopping.contact.service.ContactService;
import com.gzasc.aishopping.contact.service.impl.ContactException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/contact")
@RequiredArgsConstructor
public class UserContactController {

    private final ContactService contactService;

    @PostMapping("/create")
    public ApiResponse<Map<String, Integer>> createContact(
            @RequestBody @Valid CreateContactRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("未登录（错误代码：Co-000）");
        }
        Contact contact = toContact(request);
        int id = contactService.createContact(contact, userId);
        return ApiResponse.success("创建联系人成功", Map.of("id", id));
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteContact(
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
        return ApiResponse.success("删除联系人成功");
    }

    @PutMapping("/update")
    public ApiResponse<Void> updateContact(
            @RequestBody @Valid UpdateContactRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("更新联系人错误：未登录（错误代码：Co-000）");
        }
        Contact contact = toContact(request);
        int result = contactService.updateContact(contact, userId);
        if (result <= 0) {
            throw new ContactException("更新联系人失败：联系人不存在或无权限（错误代码：Co-010）");
        }
        return ApiResponse.success("更新联系人成功");
    }

    @GetMapping("/get/{id}")
    public ApiResponse<ContactResponse> getContactById(
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
        return ApiResponse.success("查询成功", ContactResponse.fromContact(contact));
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getContacts(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("查询联系人错误：未登录（错误代码：Co-000）");
        }
        List<Contact> contacts = contactService.getContactsByUserId(userId);
        List<ContactResponse> data = contacts.stream()
                .map(ContactResponse::fromContact)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", Map.of("contacts", data, "total", data.size()));
    }

    @PutMapping("/set-default/{id}")
    public ApiResponse<Void> setDefaultContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("设置默认联系人错误：未登录");
        }
        int result = contactService.setDefaultContact(id, userId);
        if (result <= 0) {
            throw new ContactException("设置失败：联系人不存在或不属于该用户");
        }
        return ApiResponse.success("设置成功");
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

    private Contact toContact(CreateContactRequest request) {
        Contact contact = new Contact();
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setAddress(request.getAddress());
        return contact;
    }

    private Contact toContact(UpdateContactRequest request) {
        Contact contact = new Contact();
        contact.setId(request.getId());
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setAddress(request.getAddress());
        return contact;
    }
}