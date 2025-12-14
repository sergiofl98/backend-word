package com.example.demo.config;

import com.example.demo.game.Dictionary;
import com.example.demo.handler.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final Dictionary dictionary;

    @Autowired
    public WebSocketConfig(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(dictionary), "/ws").setAllowedOrigins("*");
    }
}
