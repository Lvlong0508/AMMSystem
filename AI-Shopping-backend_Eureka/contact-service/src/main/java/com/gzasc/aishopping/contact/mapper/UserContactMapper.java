package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.Contact;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 联系人 Mapper 接口
 * 使用 contact 数据源
 */
@Mapper
public interface UserContactMapper {

    /* ========== 公共结果映射 ========== */

    @Results(id = "CONTACT_RESULT_MAPPING", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "isDefault", column = "is_default"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })

    /* ========== 查询操作 ========== */

    /**
     * 根据ID查询联系人
     */
    @Select("SELECT id, name, phone, address, is_default, created_at, updated_at FROM t_contact WHERE id = #{id}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    Contact selectContactById(int id);

    /**
     * 根据用户ID查询联系人列表（通过关联表）
     */
    @Select("SELECT c.id, c.name, c.phone, c.address, c.is_default, c.created_at, c.updated_at " +
            "FROM t_contact c " +
            "INNER JOIN user_contact uc ON c.id = uc.contact_id " +
            "WHERE uc.user_id = #{userId}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectByUserId(int userId);

    /* ========== 用户-联系人关联操作 ========== */

    /**
     * 根据用户ID查询关联的联系人的ID列表
     */
    @Select("SELECT contact_id FROM user_contact WHERE user_id = #{userId}")
    List<Integer> selectContactIdsByUserId(int userId);

    /**
     * 插入用户-联系人关联记录
     */
    @Insert("INSERT INTO user_contact (user_id, contact_id) VALUES (#{userId}, #{contactId})")
    int insertUserContact(@Param("userId") int userId, @Param("contactId") int contactId);

    /**
     * 删除指定用户-联系人关联
     */
    @Delete("DELETE FROM user_contact WHERE user_id = #{userId} AND contact_id = #{contactId}")
    int deleteByUserIdAndContactId(@Param("userId") int userId, @Param("contactId") int contactId);

    /* ========== 联系人 CRUD ========== */

    /**
     * 插入联系人
     */
    @Insert("INSERT INTO t_contact (name, phone, address, is_default) VALUES (#{name}, #{phone}, #{address}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContact(Contact contact);

    /**
     * 更新联系人
     */
    @Update("UPDATE t_contact SET name = #{name}, phone = #{phone}, address = #{address}, is_default = #{isDefault} WHERE id = #{id}")
    int updateContact(Contact contact);

    /**
     * 删除联系人
     */
    @Delete("DELETE FROM t_contact WHERE id = #{id}")
    int deleteContactById(int id);

    /**
     * 设置默认联系人
     */
    @Update("UPDATE t_contact SET is_default = 1 WHERE id = #{id}")
    int setDefaultById(int id);

    /**
     * 清除用户的所有默认联系人
     */
    @Update("UPDATE t_contact SET is_default = 0 WHERE id IN (SELECT contact_id FROM user_contact WHERE user_id = #{userId})")
    int clearDefaultByUserId(int userId);
}