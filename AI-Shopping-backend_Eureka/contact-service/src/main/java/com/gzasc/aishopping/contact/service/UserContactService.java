package com.gzasc.aishopping.contact.service;

import com.gzasc.aishopping.contact.model.Contact;

import java.util.List;

public interface UserContactService {
    int createContact(Contact contact, Long userId);
    int deleteContact(int id, Long userId);
    int updateContact(Contact contact, Long userId);
    List<Contact> getContactsByUserId(Long userId);
    int setDefaultContact(int id, Long userId);
    // 内部微服务专属
    Contact g(int id);
}