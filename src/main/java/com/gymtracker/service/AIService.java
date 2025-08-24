package com.gymtracker.service;

import com.gymtracker.entity.WorkoutLog;
import com.gymtracker.entity.MealLog;
import com.gymtracker.repository.WorkoutLogRepository;
import com.gymtracker.repository.MealLogRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AIService {

    private final WorkoutLogRepository workoutLogRepository;
    private final MealLogRepository mealLogRepository;
    private OpenAiService openAiService;

    @Autowired
    public AIService(WorkoutLogRepository workoutLogRepository, MealLogRepository mealLogRepository) {
        this.workoutLogRepository = workoutLogRepository;
        this.mealLogRepository = mealLogRepository;
    }

    public void setOpenAiApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        if (!apiKey.startsWith("sk-")) {
            throw new IllegalArgumentException("Invalid API key format. OpenAI API keys start with 'sk-'");
        }
        
        try {
            this.openAiService = new OpenAiService(apiKey);
            // Test the connection with a simple request
            ChatCompletionRequest testRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(new ChatMessage("user", "Hello")))
                    .maxTokens(10)
                    .build();
            
            openAiService.createChatCompletion(testRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate OpenAI API key: " + e.getMessage(), e);
        }
    }

    public boolean isAIConfigured() {
        return openAiService != null;
    }

    /**
     * Retrieves personalized workout recommendations based on user's exercise history.
     * Analyzes recent workouts to provide targeted suggestions for improvement.
     */
    public String getWorkoutRecommendations(Integer userId) {
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
    public String getNutritionAdvice(Integer userId) {
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
                .map(m -> String.format("Date: %s, Meal: %s, Calories: %d, Protein: %.1fg, Carbs: %.1fg, Fats: %.1fg", 
                    m.getDate(), m.getMealName(), m.getCalories(), m.getProtein(), m.getCarbs(), m.getFats()))
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
    public String getProgressAnalysis(Integer userId) {
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
     * Processes AI prompts and handles communication with OpenAI API.
     * Includes comprehensive error handling and user-friendly error messages.
     */
    private String getAIResponse(String prompt) {
        try {
            if (openAiService == null) {
                return "AI service is not properly configured. Please reconfigure your OpenAI API key.";
            }

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(500)
                    .temperature(0.7)
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
