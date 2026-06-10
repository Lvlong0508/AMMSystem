package com.gzasc.aishopping.contact.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("MerchantContact API 集成测试")
class MerchantContactApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;
    private long seq;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        seq = System.nanoTime();
    }

    private HttpHeaders headersWithShopId(String shopId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shop-Id", shopId);
        return headers;
    }

    private HttpHeaders headersWithoutShopId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private int createAddressAndGetId(String shopId) {
        var body = Map.of(
                "name", "张三",
                "phone", "13800138000",
                "address", "北京市朝阳区",
                "addressType", 1,
                "isDefault", 0
        );
        ResponseEntity<Map> resp = restTemplate.exchange(
                baseUrl + "/api/merchant/address/create", HttpMethod.POST,
                new HttpEntity<>(body, headersWithShopId(shopId)), Map.class);
        Map data = (Map) resp.getBody().get("data");
        return ((Number) data.get("id")).intValue();
    }

    // ==================== POST /api/merchant/address/create ====================

    @Test
    @DisplayName("POST /api/merchant/address/create 正常创建（addressType=1, isDefault=0）")
    void createAddress_withOptionalFields_shouldReturn200() {
        var body = Map.of(
                "name", "张三",
                "phone", "13800138000",
                "address", "北京市朝阳区",
                "addressType", 1,
                "isDefault", 0
        );
        var entity = new HttpEntity<>(body, headersWithShopId("SHP-" + seq));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        assertThat(response.getBody().get("message")).isEqualTo("新增成功");
    }

    @Test
    @DisplayName("POST /api/merchant/address/create 正常创建（isDefault=1）")
    void createAddress_withIsDefault_shouldReturn200() {
        var body = Map.of(
                "name", "张三",
                "phone", "13800138000",
                "address", "北京市朝阳区",
                "addressType", 1,
                "isDefault", 1
        );
        var entity = new HttpEntity<>(body, headersWithShopId("SHP-" + seq));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("POST /api/merchant/address/create 缺少X-Shop-Id返回401")
    void createAddress_missingHeader_shouldReturn401() {
        var body = Map.of(
                "name", "张三",
                "phone", "13800138000",
                "address", "北京市朝阳区",
                "addressType", 1
        );
        var entity = new HttpEntity<>(body, headersWithoutShopId());

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    @Test
    @DisplayName("POST /api/merchant/address/create 参数校验失败（addressType为空）返回400")
    void createAddress_validationFailed_shouldReturn400() {
        var body = Map.of(
                "name", "张三",
                "phone", "13800138000",
                "address", "北京市"
        );
        var entity = new HttpEntity<>(body, headersWithShopId("SHP-" + seq));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    // ==================== PUT /api/merchant/address/update/{id} ====================

    @Test
    @DisplayName("PUT /api/merchant/address/update/{id} 正常更新")
    void updateAddress_shouldReturn200() {
        String shopId = "SHP-" + seq;
        int id = createAddressAndGetId(shopId);

        var body = Map.of(
                "id", id,
                "name", "李四",
                "phone", "13900139000",
                "address", "上海市浦东新区",
                "addressType", 1,
                "isDefault", 0
        );
        var entity = new HttpEntity<>(body, headersWithShopId(shopId));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/update/" + id, HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        assertThat(response.getBody().get("message")).isEqualTo("修改成功");
    }

    @Test
    @DisplayName("PUT /api/merchant/address/update/{id} 缺少X-Shop-Id返回401")
    void updateAddress_missingHeader_shouldReturn401() {
        var body = Map.of(
                "id", 1,
                "name", "李四",
                "phone", "13900139000",
                "address", "上海市",
                "addressType", 1
        );
        var entity = new HttpEntity<>(body, headersWithoutShopId());

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/update/1", HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    // ==================== DELETE /api/merchant/address/delete/{id} ====================

    @Test
    @DisplayName("DELETE /api/merchant/address/delete/{id} 正常删除")
    void deleteAddress_shouldReturn200() {
        String shopId = "SHP-" + seq;
        int id = createAddressAndGetId(shopId);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/delete/" + id, HttpMethod.DELETE,
                new HttpEntity<>(headersWithShopId(shopId)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        assertThat(response.getBody().get("message")).isEqualTo("删除成功");
    }

    @Test
    @DisplayName("DELETE /api/merchant/address/delete/{id} 缺少X-Shop-Id返回401")
    void deleteAddress_missingHeader_shouldReturn401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/delete/1", HttpMethod.DELETE,
                new HttpEntity<>(headersWithoutShopId()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    // ==================== GET /api/merchant/address/list ====================

    @Test
    @DisplayName("GET /api/merchant/address/list 正常查询")
    void getAddressList_shouldReturn200() {
        String shopId = "SHP-" + seq;

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/list", HttpMethod.GET,
                new HttpEntity<>(headersWithShopId(shopId)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /api/merchant/address/list 缺少X-Shop-Id返回401")
    void getAddressList_missingHeader_shouldReturn401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/list", HttpMethod.GET,
                new HttpEntity<>(headersWithoutShopId()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    // ==================== GET /api/merchant/address/ship-default ====================

    @Test
    @DisplayName("GET /api/merchant/address/ship-default 正常查询")
    void getDefaultShipAddress_shouldReturn200() {
        String shopId = "SHP-" + seq;

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/ship-default", HttpMethod.GET,
                new HttpEntity<>(headersWithShopId(shopId)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /api/merchant/address/ship-default 缺少X-Shop-Id返回401")
    void getDefaultShipAddress_missingHeader_shouldReturn401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/ship-default", HttpMethod.GET,
                new HttpEntity<>(headersWithoutShopId()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    // ==================== PUT /api/merchant/address/set-default/{id} ====================

    @Test
    @DisplayName("PUT /api/merchant/address/set-default/{id} 正常设置默认")
    void setDefaultAddress_shouldReturn200() {
        String shopId = "SHP-" + seq;
        int id = createAddressAndGetId(shopId);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/set-default/" + id, HttpMethod.PUT,
                new HttpEntity<>(headersWithShopId(shopId)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        assertThat(response.getBody().get("message")).isEqualTo("设置成功");
    }

    @Test
    @DisplayName("PUT /api/merchant/address/set-default/{id} 缺少X-Shop-Id返回401")
    void setDefaultAddress_missingHeader_shouldReturn401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/merchant/address/set-default/1", HttpMethod.PUT,
                new HttpEntity<>(headersWithoutShopId()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }
}
