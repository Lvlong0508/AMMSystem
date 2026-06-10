package com.gzasc.aishopping.contact.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("UserContact API 集成测试")
class UserContactApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    private HttpHeaders headersWithUserId(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        return headers;
    }

    private HttpHeaders headersWithoutUserId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /** 通过 HTTP POST 创建联系人，再用 list 获取真实 id */
    private int createContactAndGetRealId(String userId) {
        var body = Map.of("name", "张三", "phone", "13800138000", "address", "北京市朝阳区");
        restTemplate.exchange(
                baseUrl + "/api/user/contact/create", HttpMethod.POST,
                new HttpEntity<>(body, headersWithUserId(userId)), Map.class);

        ResponseEntity<Map> listResp = restTemplate.exchange(
                baseUrl + "/api/user/contact/list", HttpMethod.GET,
                new HttpEntity<>(headersWithUserId(userId)), Map.class);
        Map data = (Map) listResp.getBody().get("data");
        List<Map> contacts = (List<Map>) data.get("contacts");
        return (int) contacts.get(contacts.size() - 1).get("id");
    }

    // ==================== POST /api/user/contact/create ====================

    @Test
    @DisplayName("POST /api/user/contact/create 正常创建")
    void createContact_shouldReturn200() {
        var body = Map.of("name", "张三", "phone", "13800138000", "address", "北京市朝阳区");
        var entity = new HttpEntity<>(body, headersWithUserId("1001"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        assertThat(response.getBody().get("message")).isEqualTo("创建地址成功");
    }

    @Test
    @DisplayName("POST /api/user/contact/create 缺少X-User-Id返回401")
    void createContact_missingHeader_shouldReturn401() {
        var body = Map.of("name", "张三", "phone", "13800138000", "address", "北京市朝阳区");
        var entity = new HttpEntity<>(body, headersWithoutUserId());

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
        assertThat(response.getBody().get("message")).isEqualTo("未登录");
    }

    @Test
    @DisplayName("POST /api/user/contact/create name为空返回400")
    void createContact_nameBlank_shouldReturn400() {
        var body = Map.of("name", "", "phone", "13800138000", "address", "北京市");
        var entity = new HttpEntity<>(body, headersWithUserId("1001"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    @Test
    @DisplayName("POST /api/user/contact/create phone为空返回400")
    void createContact_phoneBlank_shouldReturn400() {
        var body = Map.of("name", "张三", "phone", "", "address", "北京市");
        var entity = new HttpEntity<>(body, headersWithUserId("1001"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    @Test
    @DisplayName("POST /api/user/contact/create address为空返回400")
    void createContact_addressBlank_shouldReturn400() {
        var body = Map.of("name", "张三", "phone", "13800138000", "address", "");
        var entity = new HttpEntity<>(body, headersWithUserId("1001"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/create", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    // ==================== DELETE /api/user/contact/delete/{id} ====================

    @Test
    @DisplayName("DELETE /api/user/contact/delete/{id} 正常删除")
    void deleteContact_shouldReturn200() {
        int id = createContactAndGetRealId("2001");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/delete/" + id, HttpMethod.DELETE,
                new HttpEntity<>(headersWithUserId("2001")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        assertThat(response.getBody().get("message")).isEqualTo("删除地址成功");
    }

    @Test
    @DisplayName("DELETE /api/user/contact/delete/{id} 缺少X-User-Id返回401")
    void deleteContact_missingHeader_shouldReturn401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/delete/1", HttpMethod.DELETE,
                new HttpEntity<>(headersWithoutUserId()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    @Test
    @DisplayName("DELETE /api/user/contact/delete/{id} 删除不存在的联系人返回400")
    void deleteContact_notFound_shouldReturn400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/delete/99999", HttpMethod.DELETE,
                new HttpEntity<>(headersWithUserId("1001")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    // ==================== PUT /api/user/contact/update ====================

    @Test
    @DisplayName("PUT /api/user/contact/update 正常更新")
    void updateContact_shouldReturn200() {
        int id = createContactAndGetRealId("3001");
        var body = Map.of("id", id, "name", "李四", "phone", "13900139000", "address", "上海市浦东新区");
        var entity = new HttpEntity<>(body, headersWithUserId("3001"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/update", HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        assertThat(response.getBody().get("message")).isEqualTo("更新地址成功");
    }

    @Test
    @DisplayName("PUT /api/user/contact/update 缺少X-User-Id返回401")
    void updateContact_missingHeader_shouldReturn401() {
        var body = Map.of("id", 1, "name", "李四", "phone", "13900139000", "address", "上海市");
        var entity = new HttpEntity<>(body, headersWithoutUserId());

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/update", HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    @Test
    @DisplayName("PUT /api/user/contact/update id为空返回400")
    void updateContact_idNull_shouldReturn400() {
        var body = Map.of("name", "李四", "phone", "13900139000", "address", "上海市");
        var entity = new HttpEntity<>(body, headersWithUserId("1001"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/update", HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    @Test
    @DisplayName("PUT /api/user/contact/update 更新不存在的联系人返回400")
    void updateContact_notFound_shouldReturn400() {
        var body = Map.of("id", 99999, "name", "李四", "phone", "13900139000", "address", "上海市");
        var entity = new HttpEntity<>(body, headersWithUserId("1001"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/update", HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    @Test
    @DisplayName("PUT /api/user/contact/update 更新不属于当前用户的联系人返回400")
    void updateContact_notOwned_shouldReturn400() {
        int id = createContactAndGetRealId("4001");
        var body = Map.of("id", id, "name", "李四", "phone", "13900139000", "address", "上海市");
        var entity = new HttpEntity<>(body, headersWithUserId("99999"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/update", HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    // ==================== GET /api/user/contact/list ====================

    @Test
    @DisplayName("GET /api/user/contact/list 查询列表（有数据）")
    void listContact_hasData_shouldReturn200() {
        createContactAndGetRealId("5001");
        createContactAndGetRealId("5001");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/list", HttpMethod.GET,
                new HttpEntity<>(headersWithUserId("5001")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /api/user/contact/list 查询列表（无数据）")
    void listContact_empty_shouldReturn200() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/list", HttpMethod.GET,
                new HttpEntity<>(headersWithUserId("99999")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /api/user/contact/list 缺少X-User-Id返回401")
    void listContact_missingHeader_shouldReturn401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/list", HttpMethod.GET,
                new HttpEntity<>(headersWithoutUserId()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    // ==================== PUT /api/user/contact/set-default/{id} ====================

    @Test
    @DisplayName("PUT /api/user/contact/set-default/{id} 正常设置默认")
    void setDefaultContact_shouldReturn200() {
        int id = createContactAndGetRealId("6001");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/set-default/" + id, HttpMethod.PUT,
                new HttpEntity<>(headersWithUserId("6001")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        assertThat(response.getBody().get("message")).isEqualTo("设置成功");
    }

    @Test
    @DisplayName("PUT /api/user/contact/set-default/{id} 缺少X-User-Id返回401")
    void setDefaultContact_missingHeader_shouldReturn401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/set-default/1", HttpMethod.PUT,
                new HttpEntity<>(headersWithoutUserId()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    @Test
    @DisplayName("PUT /api/user/contact/set-default/{id} 设置不存在的联系人返回400")
    void setDefaultContact_notFound_shouldReturn400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/set-default/99999", HttpMethod.PUT,
                new HttpEntity<>(headersWithUserId("1001")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    @Test
    @DisplayName("PUT /api/user/contact/set-default/{id} 重复设置默认（幂等）")
    void setDefaultContact_alreadyDefault_shouldReturn200() {
        int id = createContactAndGetRealId("7001");
        restTemplate.exchange(
                baseUrl + "/api/user/contact/set-default/" + id, HttpMethod.PUT,
                new HttpEntity<>(headersWithUserId("7001")), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/set-default/" + id, HttpMethod.PUT,
                new HttpEntity<>(headersWithUserId("7001")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("PUT /api/user/contact/set-default/{id} 设置不属于当前用户的联系人返回400")
    void setDefaultContact_notOwned_shouldReturn400() {
        int id = createContactAndGetRealId("8001");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/user/contact/set-default/" + id, HttpMethod.PUT,
                new HttpEntity<>(headersWithUserId("99999")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }
}
