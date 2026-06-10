package com.gzasc.aishopping.auth.mapper.user;

import com.gzasc.aishopping.auth.model.UserInfo;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserInfoMapper {

    @Select("SELECT * FROM user_info WHERE id = #{id}")
    UserInfo selectById(Integer id);

    @Insert("INSERT INTO user_info (nickname, avatar, created_at, updated_at) " +
            "VALUES (#{nickname}, #{avatar}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserInfo userInfo);

    @Update("UPDATE user_info SET nickname = #{nickname}, avatar = #{avatar}, updated_at = NOW() WHERE id = #{id}")
    int update(UserInfo userInfo);
}