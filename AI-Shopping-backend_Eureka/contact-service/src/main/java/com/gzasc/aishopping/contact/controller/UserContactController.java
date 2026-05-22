package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
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
    public ApiResponse<?> createContact(
            @RequestBody @Valid CreateContactRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (bindingResult.hasErrors()) {
            return ApiResponse.error(400, "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        Long userId = getUserId(userIdStr);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Contact contact = toContact(request);
        int id = userContactService.createContact(contact, userId);
        return ApiResponse.success("创建地址成功", Map.of("id", id));
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> deleteContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = getUserId(userIdStr);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        int result = userContactService.deleteContact(id, userId);
        if (result <= 0) {
            return ApiResponse.error(400, "删除地址失败：地址不存在");
        }
        return ApiResponse.success("删除地址成功", null);
    }

    @PutMapping("/update")
    public ApiResponse<?> updateContact(
            @RequestBody @Valid UpdateContactRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (bindingResult.hasErrors()) {
            return ApiResponse.error(400, "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        Long userId = getUserId(userIdStr);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Contact contact = toContact(request);
        int result = userContactService.updateContact(contact, userId);
        if (result <= 0) {
            return ApiResponse.error(400, "更新联系人失败：地址不存在");
        }
        return ApiResponse.success("更新地址成功", null);
    }

    @GetMapping("/list")
    public ApiResponse<?> getContacts(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = getUserId(userIdStr);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        List<Contact> contacts = userContactService.getContactsByUserId(userId);
        List<ContactResponse> data = contacts.stream()
                .map(ContactResponse::fromContact)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", Map.of("contacts", data, "total", data.size()));
    }

    @PutMapping("/set-default/{id}")
    public ApiResponse<?> setDefaultContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = getUserId(userIdStr);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        int result = userContactService.setDefaultContact(id, userId);
        if (result <= 0) {
            return ApiResponse.error(400, "设置失败：地址不存在");
        }
        return ApiResponse.success("设置成功", null);
    }

    private Long getUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr);
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
