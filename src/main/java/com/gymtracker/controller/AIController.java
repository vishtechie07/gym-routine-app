package com.gymtracker.controller;

import com.gymtracker.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for AI-powered fitness insights and recommendations.
 * Provides endpoints for workout suggestions, nutrition advice, and progress analysis.
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    private final AIService aiService;

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * Configures the OpenAI API key for AI service functionality.
     * Validates the key format and tests the connection before accepting.
     */
    @PostMapping("/configure")
    public ResponseEntity<Map<String, String>> configureAI(@RequestBody Map<String, String> request) {
        String apiKey = request.get("apiKey");
        
        // Input validation and sanitization
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "API key is required"));
        }
        
        // Sanitize input
        apiKey = apiKey.trim();
        
        // Additional security checks
        if (apiKey.length() < 20 || apiKey.length() > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid API key length"));
        }
        
        // Check for suspicious patterns
        if (apiKey.contains("<script") || apiKey.contains("javascript:") || apiKey.contains("onload=")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid API key format"));
        }

        try {
            aiService.setOpenAiApiKey(apiKey);
            return ResponseEntity.ok(Map.of("message", "AI configured successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to configure AI: " + e.getMessage()));
        }
    }

    /**
     * Returns the current AI configuration status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getAIStatus() {
        return ResponseEntity.ok(Map.of("configured", aiService.isAIConfigured()));
    }

    /**
     * Retrieves personalized workout recommendations for a specific user.
     * Analyzes workout history to provide targeted exercise suggestions.
     */
    @GetMapping("/workout-recommendations/{userId}")
    public ResponseEntity<Map<String, String>> getWorkoutRecommendations(@PathVariable Integer userId) {
        try {
            String recommendations = aiService.getWorkoutRecommendations(userId);
            return ResponseEntity.ok(Map.of("recommendations", recommendations));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get recommendations: " + e.getMessage()));
        }
    }

    /**
     * Provides personalized nutrition advice based on user's dietary and workout patterns.
     */
    @GetMapping("/nutrition-advice/{userId}")
    public ResponseEntity<Map<String, String>> getNutritionAdvice(@PathVariable Integer userId) {
        try {
            String advice = aiService.getNutritionAdvice(userId);
            return ResponseEntity.ok(Map.of("advice", advice));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get nutrition advice: " + e.getMessage()));
        }
    }

    /**
     * Analyzes user's workout progress and provides insights for continued improvement.
     */
    @GetMapping("/progress-analysis/{userId}")
    public ResponseEntity<Map<String, String>> getProgressAnalysis(@PathVariable Integer userId) {
        try {
            String analysis = aiService.getProgressAnalysis(userId);
            return ResponseEntity.ok(Map.of("analysis", analysis));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get progress analysis: " + e.getMessage()));
        }
    }
    
    /**
     * Clears all stored data including API keys and session data.
     * Called when browser is closed or user wants to clear data.
     */
    @PostMapping("/clear-data")
    public ResponseEntity<Map<String, String>> clearAllData() {
        try {
            aiService.clearAllData();
            return ResponseEntity.ok(Map.of("message", "All data cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to clear data: " + e.getMessage()));
        }
    }
}
