package com.gymtracker.service;

import com.gymtracker.entity.WorkoutLog;
import com.gymtracker.entity.MealLog;
import com.gymtracker.repository.WorkoutLogRepository;
import com.gymtracker.repository.MealLogRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Service
public class AIService {

    private final WorkoutLogRepository workoutLogRepository;
    private final MealLogRepository mealLogRepository;
    private OpenAiService openAiService;
    private String encryptedApiKey;
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String SECRET_KEY = "HabitualAI2024Key"; // In production, use environment variable
    private static final SecretKeySpec AES_KEY = buildAesKey();

    private List<Map<String, Object>> motionLabCatalogCache;
    private final Object motionLabLock = new Object();

    private static SecretKeySpec buildAesKey() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, ENCRYPTION_ALGORITHM);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive AES key", e);
        }
    }

    @Autowired
    public AIService(WorkoutLogRepository workoutLogRepository, MealLogRepository mealLogRepository) {
        this.workoutLogRepository = workoutLogRepository;
        this.mealLogRepository = mealLogRepository;
    }

    public void setOpenAiApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        // Sanitize input
        apiKey = apiKey.trim();
        
        if (!apiKey.startsWith("sk-")) {
            throw new IllegalArgumentException("Invalid API key format. OpenAI API keys start with 'sk-'");
        }
        
        // Validate API key length and format
        if (apiKey.length() < 20 || apiKey.length() > 512) {
            throw new IllegalArgumentException("Invalid API key length");
        }
        
        try {
            // Encrypt and store the API key
            this.encryptedApiKey = encryptApiKey(apiKey);
            
            // Test the connection with a simple request
            this.openAiService = new OpenAiService(apiKey);
            ChatCompletionRequest testRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(new ChatMessage("user", "Hello")))
                    .maxTokens(10)
                    .build();
            
            openAiService.createChatCompletion(testRequest);
        } catch (Exception e) {
            // Clear stored key on failure
            this.encryptedApiKey = null;
            this.openAiService = null;
            throw new RuntimeException("Failed to validate OpenAI API key: " + e.getMessage(), e);
        }
    }

    public boolean isAIConfigured() {
        return openAiService != null && encryptedApiKey != null;
    }

    /**
     * Single text embedding (text-embedding-3-small). Returns null if AI unavailable or on error.
     */
    public float[] embedText(String text) {
        if (!isAIConfigured() || text == null || text.isBlank()) {
            return null;
        }
        String t = text.length() > 8000 ? text.substring(0, 8000) : text;
        List<float[]> batch = embedTextsBatch(List.of(t));
        return batch.isEmpty() ? null : batch.get(0);
    }

    /**
     * Batch embeddings; order matches input. Returns empty list on failure.
     */
    public List<float[]> embedTextsBatch(List<String> texts) {
        if (!isAIConfigured() || texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<float[]> out = new ArrayList<>();
        final int maxBatch = 100;
        try {
            for (int i = 0; i < texts.size(); i += maxBatch) {
                List<String> chunk = texts.subList(i, Math.min(i + maxBatch, texts.size()));
                List<String> trimmed = chunk.stream()
                        .map(s -> {
                            if (s == null) {
                                return "";
                            }
                            return s.length() > 8000 ? s.substring(0, 8000) : s;
                        })
                        .toList();
                EmbeddingRequest req = EmbeddingRequest.builder()
                        .model("text-embedding-3-small")
                        .input(trimmed)
                        .build();
                var result = openAiService.createEmbeddings(req);
                if (result == null || result.getData() == null) {
                    return List.of();
                }
                for (var emb : result.getData()) {
                    List<Double> e = emb.getEmbedding();
                    float[] f = new float[e.size()];
                    for (int j = 0; j < e.size(); j++) {
                        f[j] = e.get(j).floatValue();
                    }
                    out.add(f);
                }
            }
            return out;
        } catch (Exception e) {
            System.err.println("OpenAI embeddings error: " + e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> getMotionLabCatalog() {
        synchronized (motionLabLock) {
            if (motionLabCatalogCache == null) {
                try {
                    ClassPathResource res = new ClassPathResource("static/data/motion-lab-catalog.json");
                    ObjectMapper om = new ObjectMapper();
                    motionLabCatalogCache = om.readValue(res.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to load motion lab catalog", e);
                }
            }
            return motionLabCatalogCache;
        }
    }

    private String buildCompactCatalogForAI() {
        return getMotionLabCatalog().stream()
                .map(m -> m.get("id") + "|" + m.get("title") + "|" + m.get("cat") + "|" + m.get("tags"))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Ranks exercise ids from the Motion Lab catalog for a natural-language search via OpenAI.
     */
    public List<String> suggestMotionLabExerciseIds(String query) {
        if (!isAIConfigured() || query == null || query.isBlank()) {
            return List.of();
        }
        String compact = buildCompactCatalogForAI();
        String prompt = String.format("""
                You are a strength coach. Below is an exercise catalog (one line per exercise: id|title|category|search_tags).

                User search: "%s"

                Return ONLY a JSON array of exercise ids most relevant to the search, best match first. Include up to 50 ids. Use only ids from the catalog. If the query is broad, return a diverse mix. No markdown fences, no explanation — only the JSON array.

                Catalog:
                %s
                """, query.trim(), compact);

        String raw = getAIResponse(prompt, 1400, 0.25);
        raw = raw.trim();
        if (raw.startsWith("```")) {
            raw = raw.replaceFirst("^```(?:json)?\\s*", "");
            int fence = raw.lastIndexOf("```");
            if (fence >= 0) {
                raw = raw.substring(0, fence).trim();
            }
        }
        try {
            ObjectMapper om = new ObjectMapper();
            List<String> parsed = om.readValue(raw, new TypeReference<List<String>>() {});
            Set<String> allowed = getMotionLabCatalog().stream()
                    .map(m -> (String) m.get("id"))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return parsed.stream()
                    .filter(allowed::contains)
                    .limit(50)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Motion lab AI parse: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Estimates calories, macros, typical eating time, and meal slot from a meal name (OpenAI).
     */
    public Map<String, Object> estimateMealNutritionFromName(String mealName) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ai", false);
        if (!isAIConfigured() || mealName == null || mealName.isBlank()) {
            return out;
        }
        String name = mealName.trim();
        if (name.length() > 120) {
            name = name.substring(0, 120);
        }
        String prompt = String.format("""
                You are a registered dietitian assistant. Estimate typical nutrition for ONE standard serving or one plausible home/restaurant portion (not a full day).
                Food or meal: "%s"
                Reply with ONLY a JSON object (no markdown) with exactly these keys:
                "calories" (integer), "protein", "carbs", "fats" (each a number with at most one decimal),
                "mealTime" (string "HH:mm" 24-hour — typical time someone would eat this),
                "mealSlot" (one of: breakfast, lunch, dinner, snack).
                Use reasonable averages; if ambiguous pick a common preparation.
                """, name.replace("\"", "'"));
        String raw = getAIResponse(prompt, 220, 0.2);
        raw = raw.trim();
        if (raw.startsWith("```")) {
            raw = raw.replaceFirst("^```(?:json)?\\s*", "");
            int fence = raw.lastIndexOf("```");
            if (fence >= 0) {
                raw = raw.substring(0, fence).trim();
            }
        }
        try {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> parsed = om.readValue(raw, new TypeReference<Map<String, Object>>() {});
            out.put("ai", true);
            out.put("calories", clampCalories(parsed.get("calories")));
            out.put("protein", roundMacro(parsed.get("protein")));
            out.put("carbs", roundMacro(parsed.get("carbs")));
            out.put("fats", roundMacro(parsed.get("fats")));
            Object mt = parsed.get("mealTime");
            out.put("mealTime", mt != null ? mt.toString().trim() : "");
            Object slot = parsed.get("mealSlot");
            out.put("mealSlot", slot != null ? slot.toString().trim().toLowerCase() : "");
        } catch (Exception e) {
            System.err.println("Meal nutrition estimate parse: " + e.getMessage());
        }
        return out;
    }

    private static int clampCalories(Object o) {
        if (o == null) {
            return 0;
        }
        int v;
        if (o instanceof Number n) {
            v = n.intValue();
        } else {
            try {
                v = Integer.parseInt(o.toString().trim().split("\\.")[0]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return Math.max(0, Math.min(10000, v));
    }

    private static double roundMacro(Object o) {
        if (o == null) {
            return 0.0;
        }
        double v;
        if (o instanceof Number n) {
            v = n.doubleValue();
        } else {
            try {
                v = Double.parseDouble(o.toString().trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        v = Math.max(0, Math.min(999.9, v));
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
    
    /**
     * Clears all stored data including API key and service instances.
     * Called when browser is closed or user logs out.
     */
    public void clearAllData() {
        this.openAiService = null;
        this.encryptedApiKey = null;
    }
    
    /**
     * Encrypts the API key using AES encryption.
     */
    private String encryptApiKey(String apiKey) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, AES_KEY);
            byte[] encryptedBytes = cipher.doFinal(apiKey.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt API key", e);
        }
    }
    
    /**
     * Decrypts the API key using AES decryption.
     * @param encryptedApiKey The encrypted API key
     * @return The decrypted API key
     */
    @SuppressWarnings("unused")
    private String decryptApiKey(String encryptedApiKey) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, AES_KEY);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedApiKey));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt API key", e);
        }
    }

    /**
     * Retrieves personalized workout recommendations based on user's exercise history.
     * Analyzes recent workouts to provide targeted suggestions for improvement.
     */
    public String getWorkoutRecommendations(Long userId) {
        if (!isAIConfigured()) {
            return "AI is not configured. Please add your OpenAI API key in settings.";
        }

        List<WorkoutLog> recentWorkouts = workoutLogRepository.findAll().stream()
                .filter(w -> w.getUserId().equals(userId))
                .limit(10)
                .collect(Collectors.toList());

        if (recentWorkouts.isEmpty()) {
            return "No workout history found. Start logging workouts to get AI recommendations!";
        }

        String workoutHistory = recentWorkouts.stream()
                .map(w -> String.format("Date: %s, Exercise: %s, Sets: %d, Reps: %d, Weight: %.1f lbs", 
                    w.getDate(), w.getExerciseName(), w.getSets(), w.getReps(), w.getWeight()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
            As a fitness AI coach, analyze this workout history and provide recommendations:
            
            %s
            
            Please provide your response in this exact format:
            
            1. Smart exercise suggestions for next workout: [your suggestion here]
            2. Progressive overload recommendations: [your recommendation here]
            3. Alternative exercises to prevent plateaus: [your suggestion here]
            4. Recovery and rest day suggestions: [your suggestion here]
            
            Keep each point concise and actionable.
            """, workoutHistory);

        return getAIResponse(prompt);
    }

    /**
     * Provides personalized nutrition advice based on user's meal and workout data.
     * Combines dietary history with exercise patterns for comprehensive recommendations.
     */
    public String getNutritionAdvice(Long userId) {
        if (!isAIConfigured()) {
            return "AI is not configured. Please add your OpenAI API key in settings.";
        }

        List<MealLog> recentMeals = mealLogRepository.findAll().stream()
                .filter(m -> m.getUserId().equals(userId))
                .limit(7)
                .collect(Collectors.toList());

        List<WorkoutLog> recentWorkouts = workoutLogRepository.findAll().stream()
                .filter(w -> w.getUserId().equals(userId))
                .filter(w -> w.getDate().isAfter(LocalDate.now().minusDays(7)))
                .collect(Collectors.toList());

        if (recentMeals.isEmpty()) {
            return "No meal history found. Start logging meals to get AI nutrition advice!";
        }

        String mealHistory = recentMeals.stream()
                .map(m -> String.format("Date: %s, Time: %s, Meal: %s, Calories: %d, P: %.1fg, C: %.1fg, F: %.1fg",
                    m.getDate(),
                    m.getMealTime() != null ? m.getMealTime().toString() : "—",
                    m.getMealName(), m.getCalories(), m.getProtein(), m.getCarbs(), m.getFats()))
                .collect(Collectors.joining("\n"));

        String workoutSummary = recentWorkouts.stream()
                .map(w -> String.format("Date: %s, Exercise: %s, Intensity: %d sets × %d reps", 
                    w.getDate(), w.getExerciseName(), w.getSets(), w.getReps()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
            As a nutrition AI coach, analyze this data and provide advice:
            
            Recent Meals (7 days):
            %s
            
            Recent Workouts (7 days):
            %s
            
            Please provide your response in this exact format:
            
            1. Personalized meal suggestions for next few days: [your suggestion here]
            2. Macro optimization recommendations: [your recommendation here]
            3. Calorie adjustment suggestions based on workout intensity: [your suggestion here]
            4. Healthy recipe ideas: [your suggestion here]
            
            Keep each point concise and actionable.
            """, mealHistory, workoutSummary);

        return getAIResponse(prompt);
    }

    /**
     * Analyzes user's workout progress and provides insights for continued improvement.
     * Requires minimum workout data for meaningful analysis.
     */
    public String getProgressAnalysis(Long userId) {
        if (!isAIConfigured()) {
            return "AI is not configured. Please add your OpenAI API key in settings.";
        }

        List<WorkoutLog> allWorkouts = workoutLogRepository.findAll().stream()
                .filter(w -> w.getUserId().equals(userId))
                .collect(Collectors.toList());

        if (allWorkouts.size() < 5) {
            return "Need more workout data (at least 5 workouts) for meaningful progress analysis.";
        }

        // Calculate basic performance metrics
        double avgWeight = allWorkouts.stream()
                .mapToDouble(w -> w.getWeight().doubleValue())
                .average()
                .orElse(0.0);

        double avgReps = allWorkouts.stream()
                .mapToDouble(w -> w.getReps().doubleValue())
                .average()
                .orElse(0.0);

        String progressData = String.format("""
            Progress Analysis Data:
            - Total workouts: %d
            - Average weight used: %.1f lbs
            - Average reps per set: %.1f
            - Date range: %s to %s
            """, 
            allWorkouts.size(),
            avgWeight,
            avgReps,
            allWorkouts.stream().mapToLong(w -> w.getDate().toEpochDay()).min().orElse(0),
            allWorkouts.stream().mapToLong(w -> w.getDate().toEpochDay()).max().orElse(0)
        );

        String prompt = String.format("""
            As a fitness AI analyst, analyze this progress data and provide insights:
            
            %s
            
            Please provide your response in this exact format:
            
            1. Performance predictions and potential plateaus: [your insight here]
            2. Form improvement suggestions based on patterns: [your suggestion here]
            3. Goal achievement timeline estimates: [your estimate here]
            4. Personalized recommendations for continued progress: [your recommendation here]
            
            Keep each point concise and actionable.
            """, progressData);

        return getAIResponse(prompt);
    }

    /**
     * Provides AI-powered exercise name suggestions based on user input and history.
     * Combines user's workout history with intelligent exercise recommendations.
     */
    public List<String> getExerciseSuggestions(String query, Long userId) {
        if (!isAIConfigured()) {
            // Fallback to basic suggestions when AI is not configured
            return getBasicExerciseSuggestions(query);
        }

        try {
            // Get user's recent workout history for context
            List<WorkoutLog> recentWorkouts = workoutLogRepository.findAll().stream()
                    .filter(w -> userId == null || w.getUserId().equals(userId))
                    .limit(10)
                    .collect(Collectors.toList());

            String workoutContext = recentWorkouts.stream()
                    .map(w -> w.getExerciseName())
                    .distinct()
                    .collect(Collectors.joining(", "));

            String prompt = String.format("""
                As a fitness AI assistant, provide 8-12 exercise name suggestions based on this query: "%s"
                
                User's recent exercises: %s
                
                Requirements:
                1. Include exercises that match or are similar to the query
                2. Consider the user's exercise history for personalized suggestions
                3. Include variations and related exercises
                4. Provide common, well-known exercise names
                5. Order by relevance to the query
                
                Return ONLY the exercise names, one per line, no numbering or formatting.
                """, query, workoutContext.isEmpty() ? "No recent history" : workoutContext);

            String aiResponse = getAIResponse(prompt);
            
            // Parse AI response into list of suggestions
            List<String> suggestions = Arrays.stream(aiResponse.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .limit(12)
                    .collect(Collectors.toList());

            // If AI didn't provide enough suggestions, supplement with basic ones
            if (suggestions.size() < 5) {
                suggestions.addAll(getBasicExerciseSuggestions(query));
                suggestions = suggestions.stream().distinct().limit(12).collect(Collectors.toList());
            }

            return suggestions;
        } catch (Exception e) {
            // Fallback to basic suggestions on error
            return getBasicExerciseSuggestions(query);
        }
    }

    /**
     * Provides AI-powered meal name suggestions based on user input and history.
     * Combines user's meal history with intelligent food recommendations.
     */
    public List<String> getMealSuggestions(String query, Long userId) {
        if (!isAIConfigured()) {
            // Fallback to basic suggestions when AI is not configured
            return getBasicMealSuggestions(query);
        }

        try {
            // Get user's recent meal history for context
            List<MealLog> recentMeals = mealLogRepository.findAll().stream()
                    .filter(m -> userId == null || m.getUserId().equals(userId))
                    .limit(10)
                    .collect(Collectors.toList());

            String mealContext = recentMeals.stream()
                    .map(m -> m.getMealName())
                    .distinct()
                    .collect(Collectors.joining(", "));

            String prompt = String.format("""
                As a nutrition AI assistant, provide 8-12 meal/food name suggestions based on this query: "%s"
                
                User's recent meals: %s
                
                Requirements:
                1. Include foods/meals that match or are similar to the query
                2. Consider the user's meal history for personalized suggestions
                3. Include healthy alternatives and variations
                4. Provide common, recognizable food names
                5. Order by relevance to the query
                
                Return ONLY the food/meal names, one per line, no numbering or formatting.
                """, query, mealContext.isEmpty() ? "No recent history" : mealContext);

            String aiResponse = getAIResponse(prompt);
            
            // Parse AI response into list of suggestions
            List<String> suggestions = Arrays.stream(aiResponse.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .limit(12)
                    .collect(Collectors.toList());

            // If AI didn't provide enough suggestions, supplement with basic ones
            if (suggestions.size() < 5) {
                suggestions.addAll(getBasicMealSuggestions(query));
                suggestions = suggestions.stream().distinct().limit(12).collect(Collectors.toList());
            }

            return suggestions;
        } catch (Exception e) {
            // Fallback to basic suggestions on error
            return getBasicMealSuggestions(query);
        }
    }

    /**
     * Provides basic exercise suggestions as fallback when AI is not available.
     */
    private List<String> getBasicExerciseSuggestions(String query) {
        List<String> allExercises = Arrays.asList(
            "Bench Press", "Squat", "Deadlift", "Overhead Press", "Barbell Row",
            "Pull-ups", "Push-ups", "Dumbbell Press", "Incline Bench Press",
            "Decline Bench Press", "Dumbbell Flyes", "Chest Dips", "Bicep Curls",
            "Hammer Curls", "Tricep Dips", "Tricep Extensions", "Lateral Raises",
            "Front Raises", "Rear Delt Flyes", "Leg Press", "Leg Curls",
            "Leg Extensions", "Calf Raises", "Lunges", "Bulgarian Split Squats",
            "Romanian Deadlifts", "Good Mornings", "Planks", "Russian Twists",
            "Mountain Climbers", "Burpees", "Jump Squats", "Box Jumps",
            "Kettlebell Swings", "Turkish Get-ups", "Farmer's Walk", "Battle Ropes"
        );

        return allExercises.stream()
                .filter(exercise -> exercise.toLowerCase().contains(query.toLowerCase()))
                .limit(8)
                .collect(Collectors.toList());
    }

    /**
     * Provides basic meal suggestions as fallback when AI is not available.
     */
    private List<String> getBasicMealSuggestions(String query) {
        List<String> allMeals = Arrays.asList(
            "Breakfast", "Lunch", "Dinner", "Snack", "Pre-workout", "Post-workout",
            "Protein Shake", "Smoothie", "Oatmeal", "Greek Yogurt", "Eggs",
            "Chicken Breast", "Salmon", "Tuna", "Turkey", "Beef", "Pork",
            "Rice", "Quinoa", "Sweet Potato", "Brown Rice", "Pasta", "Bread",
            "Avocado", "Nuts", "Almonds", "Walnuts", "Peanut Butter", "Almond Butter",
            "Cheese", "Milk", "Cottage Cheese", "Banana", "Apple", "Berries",
            "Orange", "Broccoli", "Spinach", "Kale", "Carrots", "Green Beans",
            "Asparagus", "Salad", "Soup", "Sandwich", "Wrap", "Pizza", "Burger"
        );

        return allMeals.stream()
                .filter(meal -> meal.toLowerCase().contains(query.toLowerCase()))
                .limit(8)
                .collect(Collectors.toList());
    }

    /**
     * Processes AI prompts and handles communication with OpenAI API.
     * Includes comprehensive error handling and user-friendly error messages.
     */
    private String getAIResponse(String prompt) {
        return getAIResponse(prompt, 500, 0.7);
    }

    private String getAIResponse(String prompt, int maxTokens, double temperature) {
        try {
            if (openAiService == null) {
                return "AI service is not properly configured. Please reconfigure your OpenAI API key.";
            }

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();

            return openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("OpenAI API Error: " + e.getMessage());
            e.printStackTrace();
            
            // Provide specific error messages based on error type
            if (e.getMessage() != null) {
                if (e.getMessage().contains("401")) {
                    return "Invalid API key. Please check your OpenAI API key and try again.";
                } else if (e.getMessage().contains("429")) {
                    return "Rate limit exceeded. Please wait a moment and try again.";
                } else if (e.getMessage().contains("500")) {
                    return "OpenAI service error. Please try again later.";
                } else if (e.getMessage().contains("timeout")) {
                    return "Request timed out. Please try again.";
                } else if (e.getMessage().contains("Invalid API key")) {
                    return "Invalid API key format. Please check your OpenAI API key.";
                }
            }
            
            return "OpenAI API Error: " + e.getMessage() + ". Please check your API key and try again.";
        }
    }
}
