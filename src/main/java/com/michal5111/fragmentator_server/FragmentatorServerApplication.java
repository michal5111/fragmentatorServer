package com.michal5111.fragmentator_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class FragmentatorServerApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(FragmentatorServerApplication.class, args);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/snapshots/**").addResourceLocations("file:////var/cache/fragmenter/SpringFragmenterCache/")
                .setCachePeriod(0);
        registry.addResourceHandler("/fragments/**").addResourceLocations("file:////var/cache/fragmenter/SpringFragmenterCache/")
                .setCachePeriod(0);
    }

    @Bean
    public ServerCodecConfigurer serverCodecConfigurer() {
        return ServerCodecConfigurer.create();
    }
}
