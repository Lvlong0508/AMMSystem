package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.Shop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("ShopMapper 集成测试")
class ShopMapperTest {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static long uniqueId() {
        return System.nanoTime();
    }

    private static Shop createShop(long id, long merchantId, Integer status, Long shopInfoId) {
        Shop s = new Shop();
        s.setId(id);
        s.setMerchantId(merchantId);
        s.setShopInfoId(shopInfoId);
        s.setStatus(status != null ? status : 1);
        return s;
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入新店铺后可通过ID查询")
        void insertShop_shouldInsertAndBeQueryable() {
            Shop shop = createShop(uniqueId(), 10001L, 1, null);
            int affected = shopMapper.insertShop(shop);
            assertThat(affected).isEqualTo(1);

            Shop found = shopMapper.selectShopById(shop.getId());
            assertThat(found).isNotNull();
            assertThat(found.getMerchantId()).isEqualTo(10001L);
            assertThat(found.getStatus()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询已存在的店铺")
        void selectShopById_shouldReturnShop() {
            Shop shop = createShop(uniqueId(), 10001L, 1, null);
            shopMapper.insertShop(shop);

            Shop found = shopMapper.selectShopById(shop.getId());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(shop.getId());
        }

        @Test
        @DisplayName("查询不存在的店铺返回null")
        void selectShopById_notFound_shouldReturnNull() {
            Shop found = shopMapper.selectShopById(999999999L);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据商户ID查询店铺列表")
        void selectShopsByMerchantId_shouldReturnShops() {
            long merchantId = uniqueId();
            Shop shop1 = createShop(uniqueId(), merchantId, 1, null);
            Shop shop2 = createShop(uniqueId(), merchantId, 1, null);
            shopMapper.insertShop(shop1);
            shopMapper.insertShop(shop2);

            List<Shop> shops = shopMapper.selectShopsByMerchantId(merchantId);

            assertThat(shops).hasSize(2);
        }

        @Test
        @DisplayName("查询无店铺的商户返回空列表")
        void selectShopsByMerchantId_noResult_shouldReturnEmpty() {
            List<Shop> shops = shopMapper.selectShopsByMerchantId(999999999L);
            assertThat(shops).isEmpty();
        }

        @Test
        @DisplayName("根据用户ID查询其有权限的店铺")
        void selectShopsByUserId_shouldReturnShops() {
            long shopId = uniqueId();
            long merchantId = uniqueId();
            long userId = merchantId;
            shopMapper.insertShop(createShop(shopId, merchantId, 1, null));

            jdbcTemplate.update(
                "INSERT INTO merchant_roles (merchant_id, shop_id, role, assigned_by, created_at) VALUES (?, ?, 1, ?, NOW())",
                userId, shopId, merchantId
            );

            List<Shop> shops = shopMapper.selectShopsByUserId(userId);

            assertThat(shops).isNotEmpty();
            assertThat(shops.get(0).getId()).isEqualTo(shopId);
        }

        @Test
        @DisplayName("分页查询店铺列表")
        void selectShopsByPage_shouldReturnShops() {
            long merchantId = uniqueId();
            for (int i = 0; i < 3; i++) {
                shopMapper.insertShop(createShop(uniqueId(), merchantId, 1, null));
            }

            List<Shop> shops = shopMapper.selectShopsByPage(0);

            assertThat(shops).isNotEmpty();
        }

        @Test
        @DisplayName("查询活跃店铺列表")
        void selectActiveShops_shouldReturnActiveOnly() {
            long merchantId = uniqueId();
            long activeId = uniqueId();
            long closedId = uniqueId();
            shopMapper.insertShop(createShop(activeId, merchantId, 1, null));
            shopMapper.insertShop(createShop(closedId, merchantId, 0, null));

            List<Shop> activeShops = shopMapper.selectActiveShops(0, 10);

            assertThat(activeShops).extracting(Shop::getId).doesNotContain(closedId);
        }

        @Test
        @DisplayName("统计活跃店铺数量")
        void countActiveShops_shouldReturnCount() {
            long merchantId = uniqueId();
            shopMapper.insertShop(createShop(uniqueId(), merchantId, 1, null));
            shopMapper.insertShop(createShop(uniqueId(), merchantId, 0, null));

            int count = shopMapper.countActiveShops();

            assertThat(count).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("根据ID集合批量查询店铺")
        void selectShopsByIds_shouldReturnMatchingShops() {
            long merchantId = uniqueId();
            long id1 = uniqueId();
            long id2 = uniqueId();
            shopMapper.insertShop(createShop(id1, merchantId, 1, null));
            shopMapper.insertShop(createShop(id2, merchantId, 1, null));

            List<Shop> shops = shopMapper.selectShopsByIds(Arrays.asList(id1, id2));

            assertThat(shops).hasSize(2);
        }

        @Test
        @DisplayName("空ID集合查询返回空列表")
        void selectShopsByIds_emptyCollection_shouldReturnEmpty() {
            List<Shop> shops = shopMapper.selectShopsByIds(Collections.emptyList());
            assertThat(shops).isEmpty();
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新店铺信息")
        void updateShop_shouldUpdateFields() {
            Shop shop = createShop(uniqueId(), 10001L, 1, null);
            shopMapper.insertShop(shop);

            shop.setStatus(0);
            int affected = shopMapper.updateShop(shop);

            assertThat(affected).isEqualTo(1);
            Shop updated = shopMapper.selectShopById(shop.getId());
            assertThat(updated.getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("关闭店铺")
        void closeShop_shouldSetStatusZero() {
            Shop shop = createShop(uniqueId(), 10001L, 1, null);
            shopMapper.insertShop(shop);

            int affected = shopMapper.closeShop(shop.getId());

            assertThat(affected).isEqualTo(1);
            Shop updated = shopMapper.selectShopById(shop.getId());
            assertThat(updated.getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("开启店铺")
        void openShop_shouldSetStatusOne() {
            Shop shop = createShop(uniqueId(), 10001L, 0, null);
            shopMapper.insertShop(shop);

            int affected = shopMapper.openShop(shop.getId());

            assertThat(affected).isEqualTo(1);
            Shop updated = shopMapper.selectShopById(shop.getId());
            assertThat(updated.getStatus()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("删除店铺")
        void deleteShop_shouldRemove() {
            Shop shop = createShop(uniqueId(), 10001L, 1, null);
            shopMapper.insertShop(shop);

            int affected = shopMapper.deleteShop(shop.getId());

            assertThat(affected).isEqualTo(1);
            assertThat(shopMapper.selectShopById(shop.getId())).isNull();
        }

        @Test
        @DisplayName("删除不存在的店铺返回0")
        void deleteShop_notFound_shouldReturnZero() {
            int affected = shopMapper.deleteShop(999999999L);
            assertThat(affected).isEqualTo(0);
        }
    }
}
