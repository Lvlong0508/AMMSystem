package com.gzasc.aishopping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gzasc.aishopping.mapper")
public class AiShoppingApplication {

    public static void main(String[] args) {

        SpringApplication.run(AiShoppingApplication.class, args);
        System.out.println("后端：http://localhost:8080");
    }

}