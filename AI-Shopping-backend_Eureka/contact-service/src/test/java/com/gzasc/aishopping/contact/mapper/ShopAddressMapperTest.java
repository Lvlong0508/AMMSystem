package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.ShopAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("ShopAddressMapper 集成测试")
class ShopAddressMapperTest {

    @Autowired
    private ShopAddressMapper shopAddressMapper;

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入地址并返回自增ID")
        void insertAddress_shouldReturnGeneratedId() {
            ShopAddress addr = buildAddress("收货人A", "13800138001", "北京市朝阳区", 1, 0);
            int affected = shopAddressMapper.insertAddress(addr);

            assertThat(affected).isEqualTo(1);
            assertThat(addr.getId()).isNotNull();
        }

        @Test
        @DisplayName("插入店铺-地址关联")
        void insertRel_shouldInsertRel() {
            ShopAddress addr = insertAndReturn(buildAddress("收货人B", "13800138002", "上海市浦东新区", 1, 0));
            int affected = shopAddressMapper.insertRel("SHOP001", addr.getId());

            assertThat(affected).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询地址")
        void selectAddressById_shouldReturnAddress() {
            ShopAddress addr = insertAndReturn(buildAddress("收货人C", "13800138003", "广州市天河区", 2, 1));

            ShopAddress found = shopAddressMapper.selectAddressById(addr.getId());

            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("收货人C");
            assertThat(found.getAddressType()).isEqualTo(2);
            assertThat(found.getIsDefault()).isEqualTo(1);
        }

        @Test
        @DisplayName("查询不存在的地址返回null")
        void selectAddressById_notFound_shouldReturnNull() {
            ShopAddress found = shopAddressMapper.selectAddressById(99999);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据店铺ID查询地址列表")
        void selectAddressesByShopId_shouldReturnAddresses() {
            ShopAddress addr1 = insertAndReturn(buildAddress("收货人D1", "13800138004", "深圳市南山区", 1, 0));
            ShopAddress addr2 = insertAndReturn(buildAddress("收货人D2", "13800138005", "深圳市福田区", 2, 1));
            shopAddressMapper.insertRel("SHOP002", addr1.getId());
            shopAddressMapper.insertRel("SHOP002", addr2.getId());

            List<ShopAddress> addresses = shopAddressMapper.selectAddressesByShopId("SHOP002");

            assertThat(addresses).hasSize(2);
        }

        @Test
        @DisplayName("查询无地址的店铺返回空列表")
        void selectAddressesByShopId_noAddress_shouldReturnEmpty() {
            List<ShopAddress> addresses = shopAddressMapper.selectAddressesByShopId("NONEXISTENT");
            assertThat(addresses).isEmpty();
        }

        @Test
        @DisplayName("查询默认发货地址")
        void selectDefaultShipAddressByShopId_shouldReturn() {
            ShopAddress addr = insertAndReturn(buildAddress("收货人E", "13800138006", "杭州市西湖区", 1, 1));
            shopAddressMapper.insertRel("SHOP003", addr.getId());

            ShopAddress found = shopAddressMapper.selectDefaultShipAddressByShopId("SHOP003");

            assertThat(found).isNotNull();
            assertThat(found.getAddressType()).isEqualTo(1);
        }

        @Test
        @DisplayName("查询店铺ID通过地址ID")
        void selectShopIdByAddressId_shouldReturnShopId() {
            ShopAddress addr = insertAndReturn(buildAddress("收货人F", "13800138007", "成都市锦江区", 1, 0));
            shopAddressMapper.insertRel("SHOP004", addr.getId());

            String shopId = shopAddressMapper.selectShopIdByAddressId(addr.getId());

            assertThat(shopId).isEqualTo("SHOP004");
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新地址信息")
        void updateAddress_shouldUpdateFields() {
            ShopAddress addr = insertAndReturn(buildAddress("旧名字", "13800138008", "旧地址", 1, 0));
            addr.setName("新名字");
            addr.setAddress("新地址");
            addr.setAddressType(2);
            addr.setIsDefault(1);

            int affected = shopAddressMapper.updateAddress(addr);

            assertThat(affected).isEqualTo(1);
            ShopAddress updated = shopAddressMapper.selectAddressById(addr.getId());
            assertThat(updated.getName()).isEqualTo("新名字");
            assertThat(updated.getAddressType()).isEqualTo(2);
            assertThat(updated.getIsDefault()).isEqualTo(1);
        }

        @Test
        @DisplayName("设置默认地址")
        void setDefaultById_shouldSetDefault() {
            ShopAddress addr = insertAndReturn(buildAddress("收货人G", "13800138009", "武汉市洪山区", 1, 0));

            int affected = shopAddressMapper.setDefaultById(addr.getId());

            assertThat(affected).isEqualTo(1);
            ShopAddress updated = shopAddressMapper.selectAddressById(addr.getId());
            assertThat(updated.getIsDefault()).isEqualTo(1);
        }

        @Test
        @DisplayName("清除指定类型的默认地址")
        void clearDefaultByType_shouldClear() {
            String shopId = "IT-SHOP-" + System.nanoTime();
            ShopAddress defaultAddr = insertAndReturn(buildAddress("默认地址", "13800138010", "地址A", 1, 1));
            ShopAddress otherAddr = insertAndReturn(buildAddress("其他地址", "13800138011", "地址B", 1, 0));
            shopAddressMapper.insertRel(shopId, defaultAddr.getId());
            shopAddressMapper.insertRel(shopId, otherAddr.getId());

            int affected = shopAddressMapper.clearDefaultByType(shopId, 1);

            assertThat(affected).isEqualTo(2);
            ShopAddress cleared = shopAddressMapper.selectAddressById(defaultAddr.getId());
            assertThat(cleared.getIsDefault()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("删除地址")
        void deleteAddressById_shouldDelete() {
            ShopAddress addr = insertAndReturn(buildAddress("待删除地址", "13800138012", "待删除", 1, 0));

            int affected = shopAddressMapper.deleteAddressById(addr.getId());

            assertThat(affected).isEqualTo(1);
            assertThat(shopAddressMapper.selectAddressById(addr.getId())).isNull();
        }

        @Test
        @DisplayName("删除不存在的地址返回0")
        void deleteAddressById_notFound_shouldReturnZero() {
            int affected = shopAddressMapper.deleteAddressById(99999);
            assertThat(affected).isEqualTo(0);
        }

        @Test
        @DisplayName("删除店铺-地址关联")
        void deleteRelByAddressId_shouldDeleteRel() {
            ShopAddress addr = insertAndReturn(buildAddress("关联地址", "13800138013", "关联地址", 1, 0));
            shopAddressMapper.insertRel("SHOP006", addr.getId());

            int affected = shopAddressMapper.deleteRelByAddressId(addr.getId());

            assertThat(affected).isEqualTo(1);
            assertThat(shopAddressMapper.selectShopIdByAddressId(addr.getId())).isNull();
        }
    }

    private static ShopAddress buildAddress(String name, String phone, String address,
                                            Integer addressType, Integer isDefault) {
        ShopAddress a = new ShopAddress();
        a.setName(name);
        a.setPhone(phone);
        a.setAddress(address);
        a.setAddressType(addressType);
        a.setIsDefault(isDefault);
        return a;
    }

    private ShopAddress insertAndReturn(ShopAddress address) {
        shopAddressMapper.insertAddress(address);
        return address;
    }
}
