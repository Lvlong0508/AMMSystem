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
     * 将数据库字段映射到 Contact 实体属性
     */
    @Results(id = "CONTACT_RESULT_MAPPING", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })

    /**
     * 根据ID查询联系人
     * @param id 联系人ID
     * @return 联系人实体，未找到返回 null
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE id = #{id}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    Contact selectContactById(int id);

    /**
     * 查询所有联系人
     * @return 联系人列表
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectAllContacts();

    /**
     * 根据用户ID查询联系人列表（通过关联表）
     * @param userId 用户ID
     * @return 该用户的所有联系人
     */
    @Select("SELECT c.id, c.name, c.phone, c.address, c.created_at, c.updated_at " +
            "FROM t_contact c " +
            "INNER JOIN user_contact uc ON c.id = uc.contact_id " +
            "WHERE uc.user_id = #{userId}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectByUserId(int userId);

    /**
     * 根据姓名模糊查询联系人
     * @param name 联系人姓名
     * @return 匹配的联系人列表
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE name = #{name}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectContactsByName(String name);

    /**
     * 根据手机号查询联系人
     * @param phone 联系电话
     * @return 匹配的联系人，未找到返回 null
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE phone = #{phone}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    Contact selectContactByPhone(String phone);

    /**
     * 插入联系人
     * @param contact 联系人实体
     * @return 影响行数
     */
    @Insert("INSERT INTO t_contact (name, phone, address) VALUES (#{name}, #{phone}, #{address})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContact(Contact contact);

    /**
     * 更新联系人信息
     * @param contact 联系人实体
     * @return 影响行数
     */
    @Update("UPDATE t_contact SET name = #{name}, phone = #{phone}, address = #{address} WHERE id = #{id}")
    int updateContact(Contact contact);

    /**
     * 根据ID删除联系人
     * @param id 联系人ID
     * @return 影响行数
     */
    @Delete("DELETE FROM t_contact WHERE id = #{id}")
    int deleteContactById(int id);
}