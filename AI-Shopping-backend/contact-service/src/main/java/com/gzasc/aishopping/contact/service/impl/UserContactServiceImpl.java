package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.UserContactMapper;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.UserContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserContactServiceImpl implements UserContactService {

    private final UserContactMapper userContactMapper;

    @Override
    @Transactional
    public int createContact(Contact contact, Long userId) {
        log.info("createContact, userId={}", userId);
        if (contact.getIsDefault() == null) {
            contact.setIsDefault(0);
        }
        int result = userContactMapper.insertContact(contact);
        if (result > 0) {
            userContactMapper.insertUserRelContact(userId, contact.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public int deleteContact(int id, Long userId) {
        log.info("deleteContact, id={}, userId={}", id, userId);
        if (!isContactOwnedBy(id, userId)) {
            return 0;
        }
        userContactMapper.deleteRelByContactId(id);
        return userContactMapper.deleteContactById(id);
    }

    @Override
    @Transactional
    public int updateContact(Contact contact, Long userId) {
        log.info("updateContact, id={}, userId={}", contact.getId(), userId);
        if (contact.getIsDefault() == null) {
            contact.setIsDefault(0);
        }
        if (!isContactOwnedBy(contact.getId(), userId)) {
            return 0;
        }
        return userContactMapper.updateContact(contact);
    }

    @Override
    public List<Contact> getContactsByUserId(Long userId) {
        log.info("getContactsByUserId, userId={}", userId);
        return userContactMapper.selectByUserId(userId);
    }

    @Override
    public int setDefaultContact(int id, Long userId) {
        log.info("setDefaultContact, id={}, userId={}", id, userId);
        if (!isContactOwnedBy(id, userId)) {
            return 0;
        }
        userContactMapper.clearDefaultByUserId(userId, id);
        return userContactMapper.setDefaultById(id);
    }

    @Override
    public Contact getContactById(int id) {
        log.info("getContactById, id={}", id);
        return userContactMapper.selectContactById(id);
    }

    @Override
    public boolean isContactOwnedBy(int contactId, Long userId) {
        log.info("isContactOwnedBy, contactId={}, userId={}", contactId, userId);
        if (userId == null) return false;
        return userContactMapper.countByContactIdAndUserId(contactId, userId) > 0;
    }
}