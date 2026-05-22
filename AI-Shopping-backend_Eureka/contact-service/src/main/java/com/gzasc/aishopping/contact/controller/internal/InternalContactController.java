package com.gzasc.aishopping.contact.controller.internal;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.UserContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/contact")
@RequiredArgsConstructor
public class InternalContactController {

    private final UserContactService userContactService;

    @GetMapping("/{id}")
    public ApiResponse<Contact> getContactById(@PathVariable("id") int id) {
        Contact contact = userContactService.g(id);
        if (contact != null) {
            return ApiResponse.success(contact);
        } else {
            return ApiResponse.error(400, "联系人不存在");
        }
    }
}
