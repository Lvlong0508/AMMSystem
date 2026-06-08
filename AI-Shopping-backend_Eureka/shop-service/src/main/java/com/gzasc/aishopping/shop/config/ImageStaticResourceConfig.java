package com.gzasc.aishopping.shop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageStaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.image.resource-location:file:./AI-Shopping-backend_Eureka/static/image/}")
    private String resourceLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/image/**")
                .addResourceLocations(resourceLocation);
    }
}
