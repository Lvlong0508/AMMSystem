package com.gzasc.aishopping.auth.mapper;

import com.gzasc.aishopping.auth.mapper.user.UserInfoMapper;
import com.gzasc.aishopping.auth.model.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/eureka_auth?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.datasource.username=root",
    "spring.datasource.password=123456",
    "mybatis.configuration.map-underscore-to-camel-case=true",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@MockBean(RedisConnectionFactory.class)
@DisplayName("UserInfoMapper 集成测试")
class UserInfoMapperTest {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入用户信息并返回自增ID")
        void insert_shouldReturnGeneratedId() {
            UserInfo userInfo = new UserInfo();
            userInfo.setNickname("测试昵称");
            userInfo.setAvatar("https://example.com/avatar.png");

            int affected = userInfoMapper.insert(userInfo);
            assertThat(affected).isEqualTo(1);
            assertThat(userInfo.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询用户信息成功")
        void selectById_shouldReturnUserInfo() {
            UserInfo userInfo = insertAndReturn();

            UserInfo found = userInfoMapper.selectById(userInfo.getId());
            assertThat(found).isNotNull();
            assertThat(found.getNickname()).isEqualTo("测试昵称");
            assertThat(found.getAvatar()).isEqualTo("https://example.com/avatar.png");
        }

        @Test
        @DisplayName("查询不存在的ID返回null")
        void selectById_notFound_shouldReturnNull() {
            UserInfo found = userInfoMapper.selectById(99999);
            assertThat(found).isNull();
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新用户信息昵称和头像")
        void update_shouldModifyFields() {
            UserInfo userInfo = insertAndReturn();
            userInfo.setNickname("新昵称");
            userInfo.setAvatar("https://example.com/new-avatar.png");

            int affected = userInfoMapper.update(userInfo);
            assertThat(affected).isEqualTo(1);

            UserInfo updated = userInfoMapper.selectById(userInfo.getId());
            assertThat(updated.getNickname()).isEqualTo("新昵称");
            assertThat(updated.getAvatar()).isEqualTo("https://example.com/new-avatar.png");
        }
    }

    private UserInfo insertAndReturn() {
        UserInfo userInfo = new UserInfo();
        userInfo.setNickname("测试昵称");
        userInfo.setAvatar("https://example.com/avatar.png");
        userInfoMapper.insert(userInfo);
        return userInfo;
    }
}
