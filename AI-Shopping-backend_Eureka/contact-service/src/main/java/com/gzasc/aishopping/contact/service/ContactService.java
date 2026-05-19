package com.gzasc.aishopping.contact.service;

import com.gzasc.aishopping.contact.model.Contact;

import java.util.List;

public interface ContactService {
    int createContact(Contact contact, int userId);
    int deleteContact(int id, int userId);
    int updateContact(Contact contact, int userId);
    Contact getContactById(int id, int userId);
    Contact getContactByIdNoAuth(int id);
    List<Contact> getContactsByUserId(int userId);
    List<Contact> getContactsByName(String name);
    Contact getContactByPhone(String phone);

    /**
     * 设置默认联系人
     * @param id 联系人ID
     * @param userId 用户ID
     * @return 影响行数
     */
    int setDefaultContact(int id, int userId);
}
