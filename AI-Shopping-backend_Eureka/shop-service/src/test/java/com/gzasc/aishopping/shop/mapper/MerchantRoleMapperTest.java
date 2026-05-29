package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.MerchantRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("MerchantRoleMapper 集成测试")
class MerchantRoleMapperTest {

    @Autowired
    private MerchantRoleMapper merchantRoleMapper;

    private static long uniqueId() {
        return System.nanoTime();
    }

    private static MerchantRole createRole(long merchantId, long shopId, int role) {
        MerchantRole mr = new MerchantRole();
        mr.setMerchantId(merchantId);
        mr.setShopId(shopId);
        mr.setRole(role);
        mr.setAssignedBy(10001L);
        return mr;
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入角色并返回自增ID")
        void insert_shouldReturnGeneratedId() {
            MerchantRole mr = createRole(uniqueId(), uniqueId(), 1);
            int affected = merchantRoleMapper.insert(mr);

            assertThat(affected).isEqualTo(1);
            assertThat(mr.getId()).isNotNull();
        }

        @Test
        @DisplayName("重复merchant_id+shop_id违反唯一约束")
        void insert_duplicate_shouldThrow() {
            long merchantId = uniqueId();
            long shopId = uniqueId();
            merchantRoleMapper.insert(createRole(merchantId, shopId, 1));

            MerchantRole dup = createRole(merchantId, shopId, 2);
            assertThatThrownBy(() -> merchantRoleMapper.insert(dup))
                .isInstanceOf(DuplicateKeyException.class);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询角色")
        void selectById_shouldReturnRole() {
            MerchantRole mr = createRole(uniqueId(), uniqueId(), 1);
            merchantRoleMapper.insert(mr);

            MerchantRole found = merchantRoleMapper.selectById(mr.getId());

            assertThat(found).isNotNull();
            assertThat(found.getRole()).isEqualTo(1);
        }

        @Test
        @DisplayName("查询不存在的角色返回null")
        void selectById_notFound_shouldReturnNull() {
            MerchantRole found = merchantRoleMapper.selectById(999999999L);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据商户ID查询角色列表")
        void selectByMerchantId_shouldReturnRoles() {
            long merchantId = uniqueId();
            merchantRoleMapper.insert(createRole(merchantId, uniqueId(), 1));
            merchantRoleMapper.insert(createRole(merchantId, uniqueId(), 2));

            List<MerchantRole> roles = merchantRoleMapper.selectByMerchantId(merchantId);

            assertThat(roles).hasSize(2);
        }

        @Test
        @DisplayName("查询无角色的商户返回空列表")
        void selectByMerchantId_noResult_shouldReturnEmpty() {
            List<MerchantRole> roles = merchantRoleMapper.selectByMerchantId(999999999L);
            assertThat(roles).isEmpty();
        }

        @Test
        @DisplayName("根据店铺ID查询角色列表")
        void selectByShopId_shouldReturnRoles() {
            long shopId = uniqueId();
            merchantRoleMapper.insert(createRole(uniqueId(), shopId, 1));
            merchantRoleMapper.insert(createRole(uniqueId(), shopId, 2));

            List<MerchantRole> roles = merchantRoleMapper.selectByShopId(shopId);

            assertThat(roles).hasSize(2);
        }

        @Test
        @DisplayName("根据商户和店铺查询角色")
        void selectByMerchantAndShop_shouldReturnRole() {
            long merchantId = uniqueId();
            long shopId = uniqueId();
            merchantRoleMapper.insert(createRole(merchantId, shopId, 1));

            MerchantRole found = merchantRoleMapper.selectByMerchantAndShop(merchantId, shopId);

            assertThat(found).isNotNull();
            assertThat(found.getRole()).isEqualTo(1);
        }

        @Test
        @DisplayName("查询不存在的商户+店铺返回null")
        void selectByMerchantAndShop_notFound_shouldReturnNull() {
            MerchantRole found = merchantRoleMapper.selectByMerchantAndShop(999999999L, 999999999L);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据商户+店铺+角色精确查询")
        void selectByMerchantShopAndRole_shouldReturnRole() {
            long merchantId = uniqueId();
            long shopId = uniqueId();
            merchantRoleMapper.insert(createRole(merchantId, shopId, 1));

            MerchantRole found = merchantRoleMapper.selectByMerchantShopAndRole(merchantId, shopId, 1);

            assertThat(found).isNotNull();
            assertThat(found.getRole()).isEqualTo(1);

            MerchantRole notFound = merchantRoleMapper.selectByMerchantShopAndRole(merchantId, shopId, 2);
            assertThat(notFound).isNull();
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新角色类型")
        void updateRole_shouldUpdate() {
            MerchantRole mr = createRole(uniqueId(), uniqueId(), 1);
            merchantRoleMapper.insert(mr);

            mr.setRole(2);
            int affected = merchantRoleMapper.updateRole(mr);

            assertThat(affected).isEqualTo(1);
            MerchantRole updated = merchantRoleMapper.selectById(mr.getId());
            assertThat(updated.getRole()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("根据ID删除角色")
        void deleteById_shouldRemove() {
            MerchantRole mr = createRole(uniqueId(), uniqueId(), 1);
            merchantRoleMapper.insert(mr);

            int affected = merchantRoleMapper.deleteById(mr.getId());

            assertThat(affected).isEqualTo(1);
            assertThat(merchantRoleMapper.selectById(mr.getId())).isNull();
        }

        @Test
        @DisplayName("删除不存在的角色返回0")
        void deleteById_notFound_shouldReturnZero() {
            int affected = merchantRoleMapper.deleteById(999999999L);
            assertThat(affected).isEqualTo(0);
        }

        @Test
        @DisplayName("根据商户和店铺删除角色")
        void deleteByMerchantAndShop_shouldRemove() {
            long merchantId = uniqueId();
            long shopId = uniqueId();
            merchantRoleMapper.insert(createRole(merchantId, shopId, 1));

            int affected = merchantRoleMapper.deleteByMerchantAndShop(merchantId, shopId);

            assertThat(affected).isEqualTo(1);
            assertThat(merchantRoleMapper.selectByMerchantAndShop(merchantId, shopId)).isNull();
        }

        @Test
        @DisplayName("删除不存在的商户+店铺返回0")
        void deleteByMerchantAndShop_notFound_shouldReturnZero() {
            int affected = merchantRoleMapper.deleteByMerchantAndShop(999999999L, 999999999L);
            assertThat(affected).isEqualTo(0);
        }
    }
}
