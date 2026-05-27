package com.gzasc.aishopping.gateway;

import com.gzasc.aishopping.gateway.config.AuthWhitelistProperties;
import com.gzasc.aishopping.gateway.config.IpRateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties({AuthWhitelistProperties.class, IpRateLimitProperties.class})
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
