package com.imp3.Backend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;



@Configuration
public class WebSocketConfig {

    @Bean
    @Profile("!test") //only load outside test profile
    public ServerEndpointExporter serverEndPointExporter(){
        return new ServerEndpointExporter();
    }
}

