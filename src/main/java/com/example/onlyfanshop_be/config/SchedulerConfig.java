package com.example.onlyfanshop_be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable Spring's scheduled task execution capability.
 * This enables @Scheduled annotations throughout the application.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Configuration class to enable scheduling
    // Scheduled tasks are defined in separate scheduler classes
}
