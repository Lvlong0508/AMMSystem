package com.gzasc.aishopping.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@MockBean({RedisConnectionFactory.class, StringRedisTemplate.class})
class GatewayServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
