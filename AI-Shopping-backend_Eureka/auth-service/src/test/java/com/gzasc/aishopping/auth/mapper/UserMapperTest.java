package com.gzasc.aishopping.auth.mapper;

import com.gzasc.aishopping.auth.mapper.user.UserMapper;
import com.gzasc.aishopping.auth.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
@DisplayName("UserMapper 集成测试")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入用户返回影响行数为1")
        void insert_shouldReturnAffectedRows() {
            User user = buildUser();
            int affected = userMapper.insert(user);
            assertThat(affected).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询用户成功")
        void selectById_shouldReturnUser() {
            User user = insertAndReturn();
            User found = userMapper.selectById(user.getId());
            assertThat(found).isNotNull();
            assertThat(found.getUsername()).isEqualTo(user.getUsername());
        }

        @Test
        @DisplayName("查询不存在的ID返回null")
        void selectById_notFound_shouldReturnNull() {
            User found = userMapper.selectById(999999999L);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据用户名查询用户成功")
        void selectByUsername_shouldReturnUser() {
            User user = insertAndReturn();
            User found = userMapper.selectByUsername(user.getUsername());
            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("查询不存在的用户名返回null")
        void selectByUsername_notFound_shouldReturnNull() {
            User found = userMapper.selectByUsername("nonexistent_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("查询所有用户包含已插入记录")
        void selectAll_shouldContainInsertedUser() {
            User user = insertAndReturn();
            List<User> users = userMapper.selectAll();
            assertThat(users).isNotEmpty();
            assertThat(users).anyMatch(u -> u.getId().equals(user.getId()));
        }

        @Test
        @DisplayName("根据用户名计数存在返回1，不存在返回0")
        void countByUsername_shouldReturnCorrectCount() {
            User user = insertAndReturn();

            int count = userMapper.countByUsername(user.getUsername());
            assertThat(count).isEqualTo(1);

            int notFound = userMapper.countByUsername("not_exists_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            assertThat(notFound).isEqualTo(0);
        }

        @Test
        @DisplayName("根据手机号计数存在返回1，不存在返回0")
        void countByPhone_shouldReturnCorrectCount() {
            User user = insertAndReturn();

            int count = userMapper.countByPhone(user.getPhone());
            assertThat(count).isEqualTo(1);

            int notFound = userMapper.countByPhone("10000000000");
            assertThat(notFound).isEqualTo(0);
        }

        @Test
        @DisplayName("根据用户名查询密码返回正确值")
        void selectPasswordByUsername_shouldReturnPassword() {
            User user = insertAndReturn();

            String password = userMapper.selectPasswordByUsername(user.getUsername());
            assertThat(password).isEqualTo(user.getPassword());

            String notFound = userMapper.selectPasswordByUsername("not_exists_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            assertThat(notFound).isNull();
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新用户手机号、邮箱和状态")
        void update_shouldModifyFields() {
            User user = insertAndReturn();
            user.setPhone("137" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            user.setEmail("updated@test.com");
            user.setStatus(0);

            int affected = userMapper.update(user);
            assertThat(affected).isEqualTo(1);

            User updated = userMapper.selectById(user.getId());
            assertThat(updated.getPhone()).isEqualTo(user.getPhone());
            assertThat(updated.getEmail()).isEqualTo("updated@test.com");
            assertThat(updated.getStatus()).isEqualTo(0);
        }
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private User buildUser() {
        User user = new User();
        user.setId(System.nanoTime());
        user.setUsername("tu_" + uniqueSuffix());
        user.setPassword("$2a$12$testpassword123");
        user.setPhone("138" + uniqueSuffix());
        user.setEmail("test@example.com");
        user.setStatus(1);
        return user;
    }

    private User insertAndReturn() {
        User user = buildUser();
        userMapper.insert(user);
        return user;
    }
}
