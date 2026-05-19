package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.ContactMapper;
import com.gzasc.aishopping.contact.mapper.UserContactMapper;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.model.UserContact;
import com.gzasc.aishopping.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 联系人服务实现类
 * 提供联系人的 CRUD 操作，支持用户隔离
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMapper contactMapper;
    private final UserContactMapper userContactMapper;

    @Override
    @Transactional
    public int createContact(Contact contact, int userId) {
        log.info("createContact, userId={}", userId);
        int result = contactMapper.insertContact(contact);
        if (result > 0) {
            UserContact userContact = new UserContact();
            userContact.setUserId(userId);
            userContact.setContactId(contact.getId());
            userContactMapper.insert(userContact);
        }
        return result;
    }

    @Override
    @Transactional
    public int deleteContact(int id, int userId) {
        log.info("deleteContact, id={}, userId={}", id, userId);
        List<UserContact> userContacts = userContactMapper.selectByContactId(id);
        if (userContacts.isEmpty() || userContacts.stream().noneMatch(uc -> uc.getUserId() == userId)) {
            return 0;
        }
        userContactMapper.deleteByContactId(id);
        return contactMapper.deleteContactById(id);
    }

    @Override
    @Transactional
    public int updateContact(Contact contact, int userId) {
        log.info("updateContact, id={}, userId={}", contact.getId(), userId);
        List<UserContact> userContacts = userContactMapper.selectByContactId(contact.getId());
        if (userContacts.isEmpty() || userContacts.stream().noneMatch(uc -> uc.getUserId() == userId)) {
            return 0;
        }
        return contactMapper.updateContact(contact);
    }

    @Override
    public Contact getContactById(int id, int userId) {
        log.info("getContactById, id={}, userId={}", id, userId);
        List<UserContact> userContacts = userContactMapper.selectByContactId(id);
        if (userContacts.isEmpty() || userContacts.stream().noneMatch(uc -> uc.getUserId() == userId)) {
            return null;
        }
        return contactMapper.selectContactById(id);
    }

    @Override
    public Contact getContactByIdNoAuth(int id) {
        log.info("getContactByIdNoAuth, id={}", id);
        return contactMapper.selectContactById(id);
    }

    @Override
    public List<Contact> getContactsByUserId(int userId) {
        log.info("getContactsByUserId, userId={}", userId);
        return contactMapper.selectByUserId(userId);
    }

    @Override
    public List<Contact> getContactsByName(String name) {
        log.info("getContactsByName, name={}", name);
        return contactMapper.selectContactsByName(name);
    }

    @Override
    public Contact getContactByPhone(String phone) {
        log.info("getContactByPhone, phone={}", phone);
        return contactMapper.selectContactByPhone(phone);
    }
}