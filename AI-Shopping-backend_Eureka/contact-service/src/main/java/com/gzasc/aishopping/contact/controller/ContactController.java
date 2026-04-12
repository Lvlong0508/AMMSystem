package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

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

    @GetMapping("/get/{id}")
    public Map<String, Object> getContactById(@PathVariable("id") int id) {
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

    @GetMapping("/list")
    public Map<String, Object> getAllContacts() {
        try {
            List<Contact> contacts = contactService.getAllContacts();
            return Map.of("message", "查询成功", "data", contacts, "total", contacts.size());
        } catch (Exception e) {
            return Map.of("message", "查询联系人列表错误：" + e.getMessage());
        }
    }

    @GetMapping("/search/name")
    public Map<String, Object> getContactsByName(@RequestParam("name") String name) {
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

    @GetMapping("/search/phone")
    public Map<String, Object> getContactByPhone(@RequestParam("phone") String phone) {
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
