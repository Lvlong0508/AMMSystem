package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.Contact;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 联系人 Mapper 接口
 * 使用 contact 数据源
 */
@Mapper
public interface ContactMapper {

    /**
     * 公共结果映射
     */
    @Results(id = "CONTACT_RESULT_MAPPING", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "isDefault", column = "is_default"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })

    @Select("SELECT id, name, phone, address, is_default, created_at, updated_at FROM t_contact WHERE id = #{id}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    Contact selectContactById(int id);

    @Select("SELECT id, name, phone, address, is_default, created_at, updated_at FROM t_contact")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectAllContacts();

    @Select("SELECT c.id, c.name, c.phone, c.address, c.is_default, c.created_at, c.updated_at " +
            "FROM t_contact c " +
            "INNER JOIN user_contact uc ON c.id = uc.contact_id " +
            "WHERE uc.user_id = #{userId}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectByUserId(int userId);

    @Select("SELECT id, name, phone, address, is_default, created_at, updated_at FROM t_contact WHERE name = #{name}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectContactsByName(String name);

    @Select("SELECT id, name, phone, address, is_default, created_at, updated_at FROM t_contact WHERE phone = #{phone}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    Contact selectContactByPhone(String phone);

    @Insert("INSERT INTO t_contact (name, phone, address, is_default) VALUES (#{name}, #{phone}, #{address}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContact(Contact contact);

    @Update("UPDATE t_contact SET name = #{name}, phone = #{phone}, address = #{address}, is_default = #{isDefault} WHERE id = #{id}")
    int updateContact(Contact contact);

    @Delete("DELETE FROM t_contact WHERE id = #{id}")
    int deleteContactById(int id);

    @Update("UPDATE t_contact SET is_default = 0 WHERE user_id = #{userId}")
    int clearDefaultByUserId(int userId);

    @Update("UPDATE t_contact SET is_default = 1 WHERE id = #{id}")
    int setDefaultById(int id);
}