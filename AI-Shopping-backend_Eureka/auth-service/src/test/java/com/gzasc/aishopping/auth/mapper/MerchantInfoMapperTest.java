package com.gzasc.aishopping.auth.mapper;

import com.gzasc.aishopping.auth.mapper.merchant.MerchantInfoMapper;
import com.gzasc.aishopping.auth.model.MerchantInfo;
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
@DisplayName("MerchantInfoMapper 集成测试")
class MerchantInfoMapperTest {

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入商家信息并返回自增ID")
        void insert_shouldReturnGeneratedId() {
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setNickname("商家昵称");
            merchantInfo.setAvatar("https://example.com/merchant-avatar.png");

            int affected = merchantInfoMapper.insert(merchantInfo);
            assertThat(affected).isEqualTo(1);
            assertThat(merchantInfo.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询商家信息成功")
        void selectById_shouldReturnMerchantInfo() {
            MerchantInfo merchantInfo = insertAndReturn();

            MerchantInfo found = merchantInfoMapper.selectById(merchantInfo.getId());
            assertThat(found).isNotNull();
            assertThat(found.getNickname()).isEqualTo("商家昵称");
            assertThat(found.getAvatar()).isEqualTo("https://example.com/merchant-avatar.png");
        }

        @Test
        @DisplayName("查询不存在的ID返回null")
        void selectById_notFound_shouldReturnNull() {
            MerchantInfo found = merchantInfoMapper.selectById(99999);
            assertThat(found).isNull();
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新商家信息昵称和头像")
        void update_shouldModifyFields() {
            MerchantInfo merchantInfo = insertAndReturn();
            merchantInfo.setNickname("新商家昵称");
            merchantInfo.setAvatar("https://example.com/new-merchant-avatar.png");

            int affected = merchantInfoMapper.update(merchantInfo);
            assertThat(affected).isEqualTo(1);

            MerchantInfo updated = merchantInfoMapper.selectById(merchantInfo.getId());
            assertThat(updated.getNickname()).isEqualTo("新商家昵称");
            assertThat(updated.getAvatar()).isEqualTo("https://example.com/new-merchant-avatar.png");
        }
    }

    private MerchantInfo insertAndReturn() {
        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setNickname("商家昵称");
        merchantInfo.setAvatar("https://example.com/merchant-avatar.png");
        merchantInfoMapper.insert(merchantInfo);
        return merchantInfo;
    }
}
