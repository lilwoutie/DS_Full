package com.fooddelivery.brokerapplication.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Simple configuration to make RestTemplate available for autowiring,
 * with logging for all outgoing HTTP requests.
 */
@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        ClientHttpRequestInterceptor loggingInterceptor = (request, body, execution) -> {
            log.info("[HTTP OUT] {} {}", request.getMethod(), request.getURI());
            if (body != null && body.length > 0) {
                log.info("[HTTP OUT] Body: {}", new String(body, StandardCharsets.UTF_8));
            }
            ClientHttpResponse response = execution.execute(request, body);
            log.info("[HTTP OUT] Response status: {}", response.getStatusCode());
            return response;
        };

        restTemplate.setInterceptors(Collections.singletonList(loggingInterceptor));
        return restTemplate;
    }
}
