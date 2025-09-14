# Habitual AI - Fitness Tracking Application

A comprehensive fitness tracking web application that combines workout logging, meal tracking, and intelligent AI-powered insights to help users achieve their fitness goals. Built with modern web technologies and powered by OpenAI's GPT models for personalized fitness coaching.

## Overview

This application provides a streamlined approach to fitness tracking by combining traditional logging methods with modern AI technology. Users can track their workouts and meals through an intuitive interface, while receiving personalized recommendations and insights powered by OpenAI's GPT models.

## Core Features

### Exercise Tracking
- Log workouts with detailed information including exercise name, sets, reps, and weight
- Track progress over time with historical data
- User-specific workout history for personalized insights

### Diet Tracking
- Comprehensive meal logging with nutritional information
- Track calories, protein, carbohydrates, and fats
- Maintain dietary history for analysis and recommendations

### AI-Powered Insights
The application leverages OpenAI's GPT-3.5 Turbo to provide intelligent fitness coaching:

**Workout Recommendations**
- Exercise suggestions based on workout history and fitness level
- Progressive overload recommendations for continued improvement
- Alternative exercise suggestions to prevent plateaus
- Recovery and rest day optimization

**Nutrition Coaching**
- Personalized meal planning based on workout intensity and goals
- Macro optimization recommendations
- Calorie adjustment suggestions
- Healthy recipe recommendations

**Progress Analysis**
- Performance trend analysis and plateau detection
- Form improvement suggestions
- Goal achievement timeline estimates
- Personalized progress recommendations

### Modern User Interface
The application features a cutting-edge UI/UX design with:

**Visual Design**
- **Gradient Backgrounds** - Beautiful purple-to-blue gradients throughout the interface
- **Glass Morphism** - Translucent cards with backdrop blur effects
- **Modern Typography** - Inter font family for clean, professional text
- **Color-Coded Sections** - Each feature area has its own distinct color theme

**Interactive Elements**
- **Smooth Animations** - Fade-in, slide-in, and bounce effects for enhanced user experience
- **Hover Effects** - Cards lift and scale on hover for interactive feedback
- **Floating Elements** - Animated logo with pulsing ring effects
- **Staggered Animations** - AI cards appear with delayed timing for polished feel

**User Experience**
- **Tab-Based Navigation** - Clean, intuitive navigation between features
- **Real-time Notifications** - Animated success/error messages with smooth transitions
- **Responsive Layout** - Optimized for desktop, tablet, and mobile devices
- **Accessibility** - High contrast ratios and keyboard navigation support

## Technology Stack

### Backend
- **Java 17** - Modern Java features and performance
- **Spring Boot 3.2.0** - Rapid application development framework
- **Spring Data JPA** - Data persistence and database operations
- **Hibernate** - Object-relational mapping
- **Maven** - Dependency management and build automation

### Database
- **H2 In-Memory Database** - Lightweight database for development and testing
- **JPA/Hibernate** - Database abstraction and ORM capabilities

### Frontend
- **Vanilla JavaScript** - Modern ES6+ features without framework overhead
- **Tailwind CSS** - Utility-first CSS framework for rapid UI development
- **HTML5** - Semantic markup and modern web standards
- **Font Awesome** - Icon library for enhanced user experience
- **Modern UI/UX** - Glass morphism design with gradient backgrounds
- **Smooth Animations** - CSS animations and transitions for enhanced user experience
- **Responsive Design** - Mobile-first approach with adaptive layouts

### AI Integration
- **OpenAI GPT-3.5 Turbo** - Large language model for intelligent insights
- **OpenAI Java Client** - Official Java SDK for API integration
- **Jackson** - JSON processing and serialization

## System Requirements

- Java 17 or higher
- Maven 3.6 or higher
- OpenAI API key (for AI features)
- Modern web browser with JavaScript enabled

## Installation and Setup

### Prerequisites

1. **Java Development Kit**
   - Install Java 17 or higher
   - Verify installation: `java -version`

2. **Maven**
   - Install Maven 3.6 or higher
   - Verify installation: `mvn -version`

3. **OpenAI API Access**
   - Create account at [OpenAI Platform](https://platform.openai.com)
   - Generate API key from account settings
   - Note: API key starts with `sk-`

### Build and Run

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd gym-tracker
   ```

2. **Build Project**
   ```bash
   mvn clean install
   ```

3. **Run Application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access Application**
   - Open browser and navigate to `http://localhost:8080`
   - Default port can be modified in `application.properties`

### AI Configuration

1. Navigate to the Settings tab in the application
2. Enter your OpenAI API key in the designated field
3. Click "Configure AI" to enable intelligent features
4. Verify AI status shows "Configured" before using features

## API Reference

### Workout Management

**POST** `/api/workouts/log`
Log a new workout session.

Request Body:
```json
{
  "userId": 1,
  "date": "2025-01-24",
  "exerciseName": "Bench Press",
  "sets": 3,
  "reps": 10,
  "weight": 135.0
}
```

Response: HTTP 201 with workout details

### Meal Management

**POST** `/api/meals/log`
Log a new meal with nutritional information.

Request Body:
```json
{
  "userId": 1,
  "date": "2025-01-24",
  "mealName": "Breakfast",
  "calories": 500,
  "protein": 25.0,
  "carbs": 50.0,
  "fats": 15.0
}
```

Response: HTTP 201 with meal details

### AI Services

**POST** `/api/ai/configure`
Configure OpenAI API key for AI features.

**GET** `/api/ai/status`
Check current AI configuration status.

**GET** `/api/ai/workout-recommendations/{userId}`
Get AI-generated workout recommendations.

**GET** `/api/ai/nutrition-advice/{userId}`
Get AI-generated nutrition advice.

**GET** `/api/ai/progress-analysis/{userId}`
Get AI-generated progress analysis.

## Database Schema

The application automatically generates the following tables:

### workout_logs
- `id` (BIGINT, Primary Key)
- `user_id` (INTEGER, Not Null)
- `date` (DATE, Not Null)
- `exercise_name` (VARCHAR(100), Not Null)
- `sets` (INTEGER, Not Null)
- `reps` (INTEGER, Not Null)
- `weight` (NUMERIC(7,2), Not Null)

### meal_logs
- `id` (BIGINT, Primary Key)
- `user_id` (INTEGER, Not Null)
- `date` (DATE, Not Null)
- `meal_name` (VARCHAR(100), Not Null)
- `calories` (INTEGER, Not Null)
- `protein` (NUMERIC(5,1), Not Null)
- `carbs` (NUMERIC(5,1), Not Null)
- `fats` (NUMERIC(5,1), Not Null)

## Application Architecture

### Backend Structure
```
src/main/java/com/gymtracker/
├── GymTrackerApplication.java     # Main application class
├── controller/                    # REST API endpoints
│   ├── HomeController.java       # Root URL redirect handling
│   ├── WorkoutLogController.java # Workout management
│   ├── MealLogController.java    # Meal management
│   └── AIController.java         # AI service endpoints
├── service/                       # Business logic layer
│   ├── WorkoutLogService.java    # Workout operations
│   ├── MealLogService.java       # Meal operations
│   └── AIService.java            # AI integration
├── repository/                    # Data access layer
│   ├── WorkoutLogRepository.java # Workout persistence
│   └── MealLogRepository.java    # Meal persistence
├── entity/                        # JPA entities
│   ├── WorkoutLog.java           # Workout data model
│   └── MealLog.java              # Meal data model
└── dto/                          # Data transfer objects
    ├── WorkoutLogRequest.java    # Workout request model
    └── MealLogRequest.java       # Meal request model
```

### Frontend Structure
```
src/main/resources/
├── application.properties         # Database and application configuration
└── static/
    └── fitness-tracker.html      # Frontend application with AI features
```

## Configuration

### Application Properties
Key configuration options in `application.properties`:

- `server.port` - HTTP server port (default: 8080)
- `spring.datasource.url` - Database connection string
- `spring.jpa.hibernate.ddl-auto` - Database schema generation
- `spring.h2.console.enabled` - H2 console access
- `logging.level.com.gymtracker` - Application logging level

### AI Configuration
- API key validation on startup
- Connection testing during configuration
- Error handling for various API failure scenarios
- Secure key storage (in-memory only)

## Development Guidelines

### Code Standards
- Follow Java naming conventions
- Use meaningful variable and method names
- Implement proper exception handling
- Add comprehensive logging for debugging

### Testing
- Unit tests for service layer
- Integration tests for controllers
- API endpoint testing
- Frontend functionality testing

### Security Considerations
- Input validation on all endpoints
- API key security (not persisted)
- CORS configuration for web access
- Rate limiting considerations for AI endpoints

## Deployment

### Development Environment
- H2 in-memory database
- Spring Boot embedded Tomcat
- Hot reload for development

### Production Considerations
- Database migration to persistent storage
- Environment-specific configuration
- Monitoring and logging setup
- SSL/TLS configuration
- Load balancing for scalability

## Troubleshooting

### Common Issues

**AI Features Not Working**
- Verify OpenAI API key is configured
- Check API key format (must start with `sk-`)
- Ensure sufficient API credits
- Review application logs for errors

**Database Connection Issues**
- Verify H2 console access at `/h2-console`
- Check database configuration in properties
- Review JPA/Hibernate logs

**Frontend Issues**
- Clear browser cache
- Check browser console for JavaScript errors
- Verify all static resources are accessible

### Logging
Application logs provide detailed information about:
- Database operations
- API requests and responses
- AI service interactions
- Error conditions and stack traces

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes with proper testing
4. Submit a pull request with detailed description

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For technical support or questions:
- Review application logs for error details
- Check API documentation for endpoint usage
- Verify configuration settings
- Review troubleshooting section above
