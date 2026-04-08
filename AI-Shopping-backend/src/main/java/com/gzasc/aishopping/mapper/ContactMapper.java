package com.gzasc.aishopping.mapper;

import com.gzasc.aishopping.model.Contact;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ContactMapper {

    /**
     * 插入联系人
     */
    @Insert("INSERT INTO t_contact (name, phone, address) " +
            "VALUES (#{name}, #{phone}, #{address})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContact(Contact contact);

    /**
     * 根据ID删除联系人
     */
    @Delete("DELETE FROM t_contact WHERE id = #{id}")
    int deleteContactById(int id);

    /**
     * 更新联系人信息
     */
    @Update("UPDATE t_contact SET name = #{name}, phone = #{phone}, address = #{address} " +
            "WHERE id = #{id}")
    int updateContact(Contact contact);

    /**
     * 根据ID查询联系人
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Contact selectContactById(int id);

    /**
     * 查询所有联系人
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Contact> selectAllContacts();

    /**
     * 根据姓名查询联系人
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE name = #{name}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Contact> selectContactsByName(String name);

    /**
     * 根据电话查询联系人
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE phone = #{phone}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Contact selectContactByPhone(String phone);
}
