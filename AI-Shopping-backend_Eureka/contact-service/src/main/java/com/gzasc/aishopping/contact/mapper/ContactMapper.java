package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.Contact;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ContactMapper {

    @Insert("INSERT INTO t_contact (name, phone, address) " +
            "VALUES (#{name}, #{phone}, #{address})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContact(Contact contact);

    @Delete("DELETE FROM t_contact WHERE id = #{id}")
    int deleteContactById(int id);

    @Update("UPDATE t_contact SET name = #{name}, phone = #{phone}, address = #{address} " +
            "WHERE id = #{id}")
    int updateContact(Contact contact);

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
