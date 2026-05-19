package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.UserContact;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户-联系人关联 Mapper 接口
 * 对应 user_contact 表，处理用户与联系人的关联关系
 */
@Mapper
public interface UserContactMapper {

    /**
     * 插入用户-联系人关联记录
     * @param userContact 关联记录
     * @return 影响行数
     */
    @Insert("INSERT INTO user_contact (user_id, contact_id) VALUES (#{userId}, #{contactId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserContact userContact);

    /**
     * 根据用户ID查询关联记录
     * @param userId 用户ID
     * @return 该用户的所有联系人关联记录
     */
    @Select("SELECT * FROM user_contact WHERE user_id = #{userId}")
    List<UserContact> selectByUserId(int userId);

    /**
     * 根据联系人ID查询关联记录
     * @param contactId 联系人ID
     * @return 该联系人的所有用户关联记录
     */
    @Select("SELECT * FROM user_contact WHERE contact_id = #{contactId}")
    List<UserContact> selectByContactId(int contactId);

    /**
     * 删除指定用户-联系人关联
     * @param userId 用户ID
     * @param contactId 联系人ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_contact WHERE user_id = #{userId} AND contact_id = #{contactId}")
    int deleteByUserAndContact(@Param("userId") int userId, @Param("contactId") int contactId);

    /**
     * 根据联系人ID删除所有关联记录
     * @param contactId 联系人ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_contact WHERE contact_id = #{contactId}")
    int deleteByContactId(int contactId);
}