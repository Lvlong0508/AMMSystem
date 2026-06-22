package com.gzasc.aishopping.contact.controller.internal;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
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
    public ApiResponse<ContactDTO> getContactById(@PathVariable("id") int id) {
        Contact contact = userContactService.getContactById(id);
        if (contact != null) {
            ContactDTO dto = new ContactDTO();
            dto.setId(contact.getId());
            dto.setName(contact.getName());
            dto.setPhone(contact.getPhone());
            dto.setAddress(contact.getAddress());
            dto.setCreatedAt(contact.getCreatedAt());
            dto.setUpdatedAt(contact.getUpdatedAt());
            return ApiResponse.success(dto);
        } else {
            return ApiResponse.error(400, "联系人不存在");
        }
    }

    @GetMapping("/{contactId}/owner/{userId}")
    public ApiResponse<Boolean> validateContactOwner(@PathVariable("contactId") int contactId,
                                                      @PathVariable("userId") Long userId) {
        return ApiResponse.success(userContactService.isContactOwnedBy(contactId, userId));
    }
}
