package com.gzasc.aishopping.contact.service;

import com.gzasc.aishopping.contact.model.Contact;

import java.util.List;

public interface ContactService {
    int createContact(Contact contact);
    int deleteContact(int id);
    int updateContact(Contact contact);
    Contact getContactById(int id);
    List<Contact> getAllContacts();
    List<Contact> getContactsByName(String name);
    Contact getContactByPhone(String phone);
}
