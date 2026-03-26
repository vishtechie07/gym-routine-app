package com.gymtracker.controller;

import com.gymtracker.security.SecurityUtil;
import com.gymtracker.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    private final AIService aiService;

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/configure")
    public ResponseEntity<Map<String, String>> configureAI(@RequestBody Map<String, String> request) {
        String apiKey = request.get("apiKey");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "API key is required"));
        }

        apiKey = apiKey.trim();

        if (apiKey.length() < 20 || apiKey.length() > 512) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid API key length"));
        }

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

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getAIStatus() {
        return ResponseEntity.ok(Map.of("configured", aiService.isAIConfigured()));
    }

    @PostMapping("/meal-estimate")
    public ResponseEntity<Map<String, Object>> mealEstimate(@RequestBody Map<String, String> body) {
        String mealName = body != null && body.get("mealName") != null ? body.get("mealName").trim() : "";
        if (mealName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "mealName required", "ai", false));
        }
        if (!aiService.isAIConfigured()) {
            return ResponseEntity.ok(Map.of("ai", false, "message", "Configure OpenAI in Settings to auto-fill nutrition."));
        }
        try {
            return ResponseEntity.ok(aiService.estimateMealNutritionFromName(mealName));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ai", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/motion-lab-suggest")
    public ResponseEntity<Map<String, Object>> motionLabSuggest(@RequestBody Map<String, String> body) {
        String q = body != null && body.get("query") != null ? body.get("query").trim() : "";
        if (!aiService.isAIConfigured()) {
            return ResponseEntity.ok(Map.of("ids", List.of(), "ai", false));
        }
        try {
            List<String> ids = aiService.suggestMotionLabExerciseIds(q);
            return ResponseEntity.ok(Map.of("ids", ids, "ai", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ids", List.of(), "ai", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/workout-recommendations")
    public ResponseEntity<Map<String, String>> getWorkoutRecommendations() {
        try {
            String recommendations = aiService.getWorkoutRecommendations(SecurityUtil.currentUserId());
            return ResponseEntity.ok(Map.of("recommendations", recommendations));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get recommendations: " + e.getMessage()));
        }
    }

    @GetMapping("/nutrition-advice")
    public ResponseEntity<Map<String, String>> getNutritionAdvice() {
        try {
            String advice = aiService.getNutritionAdvice(SecurityUtil.currentUserId());
            return ResponseEntity.ok(Map.of("advice", advice));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get nutrition advice: " + e.getMessage()));
        }
    }

    @GetMapping("/progress-analysis")
    public ResponseEntity<Map<String, String>> getProgressAnalysis() {
        try {
            String analysis = aiService.getProgressAnalysis(SecurityUtil.currentUserId());
            return ResponseEntity.ok(Map.of("analysis", analysis));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get progress analysis: " + e.getMessage()));
        }
    }

    @GetMapping("/autocomplete/exercises")
    public ResponseEntity<Map<String, Object>> getExerciseSuggestions(@RequestParam String query) {
        try {
            List<String> suggestions = aiService.getExerciseSuggestions(query, SecurityUtil.currentUserId());
            return ResponseEntity.ok(Map.of("suggestions", suggestions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get exercise suggestions: " + e.getMessage()));
        }
    }

    @GetMapping("/autocomplete/meals")
    public ResponseEntity<Map<String, Object>> getMealSuggestions(@RequestParam String query) {
        try {
            List<String> suggestions = aiService.getMealSuggestions(query, SecurityUtil.currentUserId());
            return ResponseEntity.ok(Map.of("suggestions", suggestions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get meal suggestions: " + e.getMessage()));
        }
    }

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
