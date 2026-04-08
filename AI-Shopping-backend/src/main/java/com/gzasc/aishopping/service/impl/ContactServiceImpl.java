package com.gzasc.aishopping.service.impl;

import com.gzasc.aishopping.mapper.ContactMapper;
import com.gzasc.aishopping.model.Contact;
import com.gzasc.aishopping.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactMapper contactMapper;

    @Override
    public int createContact(Contact contact) {
        System.out.println(new Date() + ": run createContact");
        try {
            return contactMapper.insertContact(contact);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int deleteContact(int id) {
        System.out.println(new Date() + ": run deleteContact");
        return contactMapper.deleteContactById(id);
    }

    @Override
    public int updateContact(Contact contact) {
        System.out.println(new Date() + ": run updateContact");
        return contactMapper.updateContact(contact);
    }

    @Override
    public Contact getContactById(int id) {
        System.out.println(new Date() + ": run getContactById");
        return contactMapper.selectContactById(id);
    }

    @Override
    public List<Contact> getAllContacts() {
        System.out.println(new Date() + ": run getAllContacts");
        return contactMapper.selectAllContacts();
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
