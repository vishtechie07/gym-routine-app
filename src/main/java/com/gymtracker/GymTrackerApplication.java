package com.gymtracker;

import com.gymtracker.config.LabYoutubeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main application class for Habitual AI - Fitness Tracking Application.
 * Provides comprehensive fitness tracking capabilities with AI-powered insights and recommendations.
 */
@SpringBootApplication
@EnableConfigurationProperties(LabYoutubeProperties.class)
public class GymTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GymTrackerApplication.class, args);
    }
}
