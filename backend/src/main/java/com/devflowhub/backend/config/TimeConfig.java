package com.devflowhub.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    // A Clock bean makes timer logic easier to test.
    @Bean
    public Clock applicationClock() {
        return Clock.systemDefaultZone();
    }
}
