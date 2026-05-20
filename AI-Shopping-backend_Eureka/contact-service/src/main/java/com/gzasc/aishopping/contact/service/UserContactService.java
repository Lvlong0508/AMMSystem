package com.gzasc.aishopping.contact.service;

import com.gzasc.aishopping.contact.model.Contact;

import java.util.List;

public interface UserContactService {
    int createContact(Contact contact, int userId);
    int deleteContact(int id, int userId);
    int updateContact(Contact contact, int userId);
    List<Contact> getContactsByUserId(int userId);
    int setDefaultContact(int id, int userId);
    // 内部微服务专属
    Contact g(int id);
}