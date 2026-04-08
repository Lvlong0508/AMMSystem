package com.gzasc.aishopping.service;

import com.gzasc.aishopping.model.Contact;

import java.util.List;

public interface ContactService {

    /**
     * 创建联系人
     * @param contact 联系人信息
     */
    int createContact(Contact contact);

    /**
     * 删除联系人
     */
    int deleteContact(int id);

    /**
     * 更新联系人信息
     */
    int updateContact(Contact contact);

    /**
     * 根据ID查询联系人
     */
    Contact getContactById(int id);

    /**
     * 查询所有联系人
     */
    List<Contact> getAllContacts();

    /**
     * 根据姓名查询联系人
     */
    List<Contact> getContactsByName(String name);

    /**
     * 根据电话查询联系人
     */
    Contact getContactByPhone(String phone);
}
