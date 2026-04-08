package com.gzasc.aishopping.controller;

import com.gzasc.aishopping.model.Contact;
import com.gzasc.aishopping.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * 创建联系人
     */
    @PostMapping("/create")
    public Map<String, String> createContact(@RequestBody Contact contact) {
        if (contact == null || contact.getName() == null || contact.getName().trim().isEmpty()) {
            return Map.of("message", "创建联系人错误：姓名为空（错误代码：Co-001）");
        }
        if (contact.getPhone() == null || contact.getPhone().trim().isEmpty()) {
            return Map.of("message", "创建联系人错误：电话为空（错误代码：Co-002）");
        }
        if (contact.getAddress() == null || contact.getAddress().trim().isEmpty()) {
            return Map.of("message", "创建联系人错误：地址为空（错误代码：Co-003）");
        }
        try {
            int result = contactService.createContact(contact);
            if (result > 0) {
                return Map.of("message", "创建联系人成功", "id", String.valueOf(contact.getId()));
            } else {
                return Map.of("message", "创建联系人失败（错误代码：Co-004）");
            }
        } catch (Exception e) {
            return Map.of("message", "创建联系人错误：" + e.getMessage());
        }
    }

    /**
     * 删除联系人
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, String> deleteContact(@PathVariable int id) {
        try {
            int result = contactService.deleteContact(id);
            if (result > 0) {
                return Map.of("message", "删除联系人成功");
            } else {
                return Map.of("message", "删除联系人失败：联系人不存在（错误代码：Co-005）");
            }
        } catch (Exception e) {
            return Map.of("message", "删除联系人错误：" + e.getMessage());
        }
    }

    /**
     * 更新联系人
     */
    @PutMapping("/update")
    public Map<String, String> updateContact(@RequestBody Contact contact) {
        if (contact == null || contact.getId() <= 0) {
            return Map.of("message", "更新联系人错误：ID无效（错误代码：Co-006）");
        }
        if (contact.getName() == null || contact.getName().trim().isEmpty()) {
            return Map.of("message", "更新联系人错误：姓名为空（错误代码：Co-007）");
        }
        if (contact.getPhone() == null || contact.getPhone().trim().isEmpty()) {
            return Map.of("message", "更新联系人错误：电话为空（错误代码：Co-008）");
        }
        if (contact.getAddress() == null || contact.getAddress().trim().isEmpty()) {
            return Map.of("message", "更新联系人错误：地址为空（错误代码：Co-009）");
        }
        try {
            int result = contactService.updateContact(contact);
            if (result > 0) {
                return Map.of("message", "更新联系人成功");
            } else {
                return Map.of("message", "更新联系人失败：联系人不存在（错误代码：Co-010）");
            }
        } catch (Exception e) {
            return Map.of("message", "更新联系人错误：" + e.getMessage());
        }
    }
}
