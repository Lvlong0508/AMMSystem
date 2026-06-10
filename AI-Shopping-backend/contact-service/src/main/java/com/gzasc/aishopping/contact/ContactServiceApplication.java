package com.gzasc.aishopping.contact;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.gzasc.aishopping.contact.mapper")
public class ContactServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContactServiceApplication.class, args);
        System.out.println("Contact Service started at http://localhost:8083");
    }
}
