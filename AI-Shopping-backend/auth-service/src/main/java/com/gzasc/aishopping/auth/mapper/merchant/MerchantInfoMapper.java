package com.gzasc.aishopping.auth.mapper.merchant;

import com.gzasc.aishopping.auth.model.MerchantInfo;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MerchantInfoMapper {

    @Select("SELECT * FROM merchant_info WHERE id = #{id}")
    MerchantInfo selectById(Integer id);

    @Insert("INSERT INTO merchant_info (nickname, avatar, created_at, updated_at) " +
            "VALUES (#{nickname}, #{avatar}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MerchantInfo merchantInfo);

    @Update("UPDATE merchant_info SET nickname = #{nickname}, avatar = #{avatar}, updated_at = NOW() WHERE id = #{id}")
    int update(MerchantInfo merchantInfo);
}