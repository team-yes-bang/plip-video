package com.plip.video.support;

import com.plip.video.global.config.GatewayHmacProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class WebMvcSecurityTestConfig {

	@Bean
	GatewayHmacProperties gatewayHmacProperties() {
		return new GatewayHmacProperties("", 300L);
	}

}
