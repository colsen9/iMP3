package com.imp3.Backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {

        // this is a springboot thing that can log incoming http requests
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();

        // include a bunch of stuff
        filter.setIncludeQueryString(true);
        filter.setIncludeClientInfo(true);
        filter.setIncludeHeaders(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(1_000);  // 1000 characters or lines?

        // return filter
        return filter;
    }
}