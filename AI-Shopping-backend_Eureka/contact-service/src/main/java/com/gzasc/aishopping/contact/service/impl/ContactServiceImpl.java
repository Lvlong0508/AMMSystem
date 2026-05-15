package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.ContactMapper;
import com.gzasc.aishopping.contact.mapper.UserContactMapper;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.model.UserContact;
import com.gzasc.aishopping.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMapper contactMapper;
    private final UserContactMapper userContactMapper;

    @Override
    public int createContact(Contact contact, int userId) {
        System.out.println(new Date() + ": run createContact, userId=" + userId);
        try {
            int result = contactMapper.insertContact(contact);
            if (result > 0) {
                UserContact userContact = new UserContact();
                userContact.setUserId(userId);
                userContact.setContactId(contact.getId());
                userContactMapper.insert(userContact);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int deleteContact(int id, int userId) {
        System.out.println(new Date() + ": run deleteContact, id=" + id + ", userId=" + userId);
        List<UserContact> userContacts = userContactMapper.selectByContactId(id);
        if (userContacts.isEmpty() || userContacts.stream().noneMatch(uc -> uc.getUserId() == userId)) {
            return 0;
        }
        userContactMapper.deleteByContactId(id);
        return contactMapper.deleteContactById(id);
    }

    @Override
    public int updateContact(Contact contact, int userId) {
        System.out.println(new Date() + ": run updateContact, id=" + contact.getId() + ", userId=" + userId);
        List<UserContact> userContacts = userContactMapper.selectByContactId(contact.getId());
        if (userContacts.isEmpty() || userContacts.stream().noneMatch(uc -> uc.getUserId() == userId)) {
            return 0;
        }
        return contactMapper.updateContact(contact);
    }

    @Override
    public Contact getContactById(int id, int userId) {
        System.out.println(new Date() + ": run getContactById, id=" + id + ", userId=" + userId);
        List<UserContact> userContacts = userContactMapper.selectByContactId(id);
        if (userContacts.isEmpty() || userContacts.stream().noneMatch(uc -> uc.getUserId() == userId)) {
            return null;
        }
        return contactMapper.selectContactById(id);
    }

    @Override
    public List<Contact> getContactsByUserId(int userId) {
        System.out.println(new Date() + ": run getContactsByUserId, userId=" + userId);
        return contactMapper.selectByUserId(userId);
    }

    @Override
    public List<Contact> getContactsByName(String name) {
        System.out.println(new Date() + ": run getContactsByName");
        return contactMapper.selectContactsByName(name);
    }

    @Override
    public Contact getContactByPhone(String phone) {
        System.out.println(new Date() + ": run getContactByPhone");
        return contactMapper.selectContactByPhone(phone);
    }
}
