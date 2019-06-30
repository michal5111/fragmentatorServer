package com.michal5111.fragmentatorServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class FragmentatorServerApplication implements WebMvcConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(FragmentatorServerApplication.class, args);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/snapshots/**").addResourceLocations("file:////home/michal/Obrazy/SpringFragmenterCache/")
				.setCachePeriod(0);
		registry.addResourceHandler("/fragments/**").addResourceLocations("file:////home/michal/Wideo/SpringFragmenterCache/")
				.setCachePeriod(0);
	}

}
