package com.r29kta.cookiehandler.io;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public Queue cookie() {
        return new Queue("cookie");
    }
    @Bean
    public Queue payload() {
        return new Queue("payload");
    }
    @Bean
    public Queue log() {
        return new Queue("log");
    }
}
