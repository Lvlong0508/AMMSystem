package com.gzasc.aishopping.auth.mapper;

import com.gzasc.aishopping.auth.mapper.merchant.MerchantInfoMapper;
import com.gzasc.aishopping.auth.mapper.merchant.MerchantMapper;
import com.gzasc.aishopping.auth.model.Merchant;
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
@DisplayName("MerchantMapper 集成测试")
class MerchantMapperTest {

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入商家返回影响行数为1")
        void insert_shouldReturnAffectedRows() {
            Merchant merchant = buildMerchant();
            int affected = merchantMapper.insert(merchant);
            assertThat(affected).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询商家成功")
        void selectById_shouldReturnMerchant() {
            Merchant merchant = insertAndReturn();
            Merchant found = merchantMapper.selectById(merchant.getId());
            assertThat(found).isNotNull();
            assertThat(found.getUsername()).isEqualTo(merchant.getUsername());
        }

        @Test
        @DisplayName("查询不存在的ID返回null")
        void selectById_notFound_shouldReturnNull() {
            Merchant found = merchantMapper.selectById(999999999L);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据用户名查询商家成功")
        void selectByUsername_shouldReturnMerchant() {
            Merchant merchant = insertAndReturn();
            Merchant found = merchantMapper.selectByUsername(merchant.getUsername());
            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(merchant.getId());
        }

        @Test
        @DisplayName("查询不存在的用户名返回null")
        void selectByUsername_notFound_shouldReturnNull() {
            Merchant found = merchantMapper.selectByUsername("nonexistent_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("查询所有商家包含已插入记录")
        void selectAll_shouldContainInsertedMerchant() {
            Merchant merchant = insertAndReturn();
            List<Merchant> merchants = merchantMapper.selectAll();
            assertThat(merchants).isNotEmpty();
            assertThat(merchants).anyMatch(m -> m.getId().equals(merchant.getId()));
        }

        @Test
        @DisplayName("根据用户名计数存在返回1，不存在返回0")
        void countByUsername_shouldReturnCorrectCount() {
            Merchant merchant = insertAndReturn();

            int count = merchantMapper.countByUsername(merchant.getUsername());
            assertThat(count).isEqualTo(1);

            int notFound = merchantMapper.countByUsername("not_exists_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            assertThat(notFound).isEqualTo(0);
        }

        @Test
        @DisplayName("根据手机号计数存在返回1，不存在返回0")
        void countByPhone_shouldReturnCorrectCount() {
            Merchant merchant = insertAndReturn();

            int count = merchantMapper.countByPhone(merchant.getPhone());
            assertThat(count).isEqualTo(1);

            int notFound = merchantMapper.countByPhone("10000000000");
            assertThat(notFound).isEqualTo(0);
        }

        @Test
        @DisplayName("根据用户名查询密码返回正确值")
        void selectPasswordByUsername_shouldReturnPassword() {
            Merchant merchant = insertAndReturn();

            String password = merchantMapper.selectPasswordByUsername(merchant.getUsername());
            assertThat(password).isEqualTo(merchant.getPassword());

            String notFound = merchantMapper.selectPasswordByUsername("not_exists_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            assertThat(notFound).isNull();
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新商家手机号、邮箱和状态")
        void update_shouldModifyFields() {
            Merchant merchant = insertAndReturn();
            merchant.setPhone("137" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            merchant.setEmail("updated@test.com");
            merchant.setStatus(0);

            int affected = merchantMapper.update(merchant);
            assertThat(affected).isEqualTo(1);

            Merchant updated = merchantMapper.selectById(merchant.getId());
            assertThat(updated.getPhone()).isEqualTo(merchant.getPhone());
            assertThat(updated.getEmail()).isEqualTo("updated@test.com");
            assertThat(updated.getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("根据ID更新商家infoId")
        void updateById_shouldModifyInfoId() {
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setNickname("商家信息");
            merchantInfoMapper.insert(merchantInfo);

            Merchant merchant = insertAndReturn();
            merchant.setInfoId(merchantInfo.getId());

            int affected = merchantMapper.updateById(merchant);
            assertThat(affected).isEqualTo(1);

            Merchant updated = merchantMapper.selectById(merchant.getId());
            assertThat(updated.getInfoId()).isEqualTo(merchantInfo.getId());
        }
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private Merchant buildMerchant() {
        Merchant merchant = new Merchant();
        merchant.setId(System.nanoTime());
        merchant.setUsername("tm_" + uniqueSuffix());
        merchant.setPassword("$2a$12$testpassword123");
        merchant.setPhone("138" + uniqueSuffix());
        merchant.setEmail("merchant@test.com");
        merchant.setStatus(1);
        return merchant;
    }

    private Merchant insertAndReturn() {
        Merchant merchant = buildMerchant();
        merchantMapper.insert(merchant);
        return merchant;
    }
}
