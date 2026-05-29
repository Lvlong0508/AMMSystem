package com.gzasc.aishopping.shop.mapper;

import com.gzasc.aishopping.shop.model.ShopInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("ShopInfoMapper 集成测试")
class ShopInfoMapperTest {

    @Autowired
    private ShopInfoMapper shopInfoMapper;

    private static long uniqueId() {
        return System.nanoTime();
    }

    private static ShopInfo createShopInfo(long id, String name) {
        ShopInfo si = new ShopInfo();
        si.setId(id);
        si.setName(name);
        si.setDescription("测试描述");
        si.setLogoUrl("https://example.com/logo.png");
        si.setAddress("北京市朝阳区");
        si.setPhone("13800138000");
        return si;
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入新店铺信息后可通过ID查询")
        void insert_shouldInsertAndBeQueryable() {
            ShopInfo si = createShopInfo(uniqueId(), "测试店铺");

            int affected = shopInfoMapper.insert(si);

            assertThat(affected).isEqualTo(1);
            ShopInfo found = shopInfoMapper.selectById(si.getId());
            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("测试店铺");
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询已存在的店铺信息")
        void selectById_shouldReturnShopInfo() {
            ShopInfo si = createShopInfo(uniqueId(), "测试店铺");
            shopInfoMapper.insert(si);

            ShopInfo found = shopInfoMapper.selectById(si.getId());

            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("测试店铺");
        }

        @Test
        @DisplayName("查询不存在的店铺信息返回null")
        void selectById_notFound_shouldReturnNull() {
            ShopInfo found = shopInfoMapper.selectById(999999999L);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("批量查询店铺信息")
        void selectBatch_shouldReturnList() {
            long id1 = uniqueId();
            long id2 = uniqueId();
            shopInfoMapper.insert(createShopInfo(id1, "店铺A"));
            shopInfoMapper.insert(createShopInfo(id2, "店铺B"));

            List<ShopInfo> list = shopInfoMapper.selectBatch(Arrays.asList(id1, id2));

            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("批量查询空集合返回空列表")
        void selectBatch_emptyIds_shouldReturnEmpty() {
            List<ShopInfo> list = shopInfoMapper.selectBatch(Collections.emptyList());
            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("批量查询部分匹配")
        void selectBatch_partialMatch() {
            long existingId = uniqueId();
            shopInfoMapper.insert(createShopInfo(existingId, "存在"));

            List<ShopInfo> list = shopInfoMapper.selectBatch(Arrays.asList(existingId, 999999999L));

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getName()).isEqualTo("存在");
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新店铺信息")
        void update_shouldUpdateFields() {
            ShopInfo si = createShopInfo(uniqueId(), "旧名称");
            shopInfoMapper.insert(si);

            si.setName("新名称");
            si.setDescription("新描述");
            int affected = shopInfoMapper.update(si);

            assertThat(affected).isEqualTo(1);
            ShopInfo updated = shopInfoMapper.selectById(si.getId());
            assertThat(updated.getName()).isEqualTo("新名称");
            assertThat(updated.getDescription()).isEqualTo("新描述");
        }
    }
}
