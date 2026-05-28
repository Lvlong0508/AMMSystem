package com.gzasc.aishopping.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest
@MockBean(RedisConnectionFactory.class)
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
