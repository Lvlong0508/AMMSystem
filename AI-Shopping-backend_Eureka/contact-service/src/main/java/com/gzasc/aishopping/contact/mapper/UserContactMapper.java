package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.UserContact;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserContactMapper {

    @Insert("INSERT INTO user_contact (user_id, contact_id) VALUES (#{userId}, #{contactId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserContact userContact);

    @Select("SELECT * FROM user_contact WHERE user_id = #{userId}")
    List<UserContact> selectByUserId(int userId);

    @Select("SELECT * FROM user_contact WHERE contact_id = #{contactId}")
    List<UserContact> selectByContactId(int contactId);

    @Delete("DELETE FROM user_contact WHERE user_id = #{userId} AND contact_id = #{contactId}")
    int deleteByUserAndContact(@Param("userId") int userId, @Param("contactId") int contactId);

    @Delete("DELETE FROM user_contact WHERE contact_id = #{contactId}")
    int deleteByContactId(int contactId);
}