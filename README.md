# Habitual AI — Fitness Tracking Application

Workout logging + meal logging (including `mealTime`) + AI-driven nutrition and insights.

The frontend is served from the backend as a static page: `GET /fitness-tracker.html`.

## Features

### Workouts
- Create workout logs: date, exercise name, sets, reps, weight.
- Endpoint: `POST /api/workouts/log`

### Diet (Meals)
- Create, list, update, and delete meal logs.
- `mealTime` is optional:
  - create: empty uses server current time
  - update: empty keeps the stored `mealTime`
- Endpoints:
  - `POST /api/meals/log`
  - `GET  /api/meals/list?date=YYYY-MM-DD` (defaults to today)
  - `PUT  /api/meals/{id}`
  - `DELETE /api/meals/{id}`

### AI
- Configure OpenAI API key in the Settings tab (`POST /api/ai/configure`).
- AI endpoints (JWT required):
  - `GET /api/ai/status`
  - `POST /api/ai/meal-estimate` (estimates calories/macros + meal time + meal slot from meal name)
  - `GET /api/ai/workout-recommendations`
  - `GET /api/ai/nutrition-advice`
  - `GET /api/ai/progress-analysis`
  - `GET /api/ai/autocomplete/exercises?query=...`
  - `GET /api/ai/autocomplete/meals?query=...`
  - `POST /api/ai/clear-data`

### Motion Lab (Tutorials)
- YouTube ingest + optional embeddings (only when AI is configured).
- Endpoints:
  - `POST /api/lab/sync`
  - `GET  /api/lab/search?q=...&channelId=...&limit=48`
  - `GET  /api/lab/channels`
  - `GET  /api/lab/status`

### Dashboard
- `GET /api/dashboard/summary`

## Quick Start

### Prerequisites
- Java 17+
- Maven
- Docker (optional, for Postgres)

### Run with Postgres (default)
1. Start the database:
   ```bash
   docker compose up -d
   ```
2. Start the app:
   ```bash
   mvn spring-boot:run
   ```
3. Open:
   - `http://localhost:8080/fitness-tracker.html`

Postgres credentials are defined in `docker-compose.yml`.

### Run with H2 (no Docker)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Then open:
- `http://localhost:8080/fitness-tracker.html`
- H2 console: `http://localhost:8080/h2-console`

## Configuration

### JWT
- The app uses JWT for auth.
- `app.jwt.secret` is configured in `src/main/resources/application.properties`.

### OpenAI
- Configure your OpenAI key from the Settings tab.
- The key is validated by the server and kept in memory until restart.

### YouTube Data API (Motion Lab)
- Configure `lab.youtube.api-key` via env var `YOUTUBE_API_KEY` or `application-local.properties` (gitignored).

## API Reference

### Auth (no JWT)
- `POST /api/auth/register`
- `POST /api/auth/login`

### Dashboard (JWT required)
- `GET /api/dashboard/summary`

### Workouts (JWT required)
- `POST /api/workouts/log`
- Request body:
  ```json
  {
    "date": "2026-03-26",
    "exerciseName": "Bench Press",
    "sets": 3,
    "reps": 10,
    "weight": 135.0
  }
  ```

### Meals / Diet (JWT required)
- `POST /api/meals/log`
- `GET /api/meals/list?date=YYYY-MM-DD` (defaults to today)
- `PUT /api/meals/{id}`
- `DELETE /api/meals/{id}`

- Request body:
  ```json
  {
    "date": "2026-03-26",
    "mealName": "Greek yogurt with berries",
    "calories": 420,
    "protein": 25.0,
    "carbs": 35.0,
    "fats": 12.5,
    "mealTime": "08:15"
  }
  ```

`mealTime` is optional; it must be `HH:mm` when provided (empty string allowed).
For `PUT`, an empty `mealTime` keeps the stored time.

### AI (JWT required)
- `POST /api/ai/configure`
- `GET /api/ai/status`
- `POST /api/ai/meal-estimate` (fills calories/macros + mealTime + mealSlot from meal name)
- `GET /api/ai/workout-recommendations`
- `GET /api/ai/nutrition-advice`
- `GET /api/ai/progress-analysis`
- `GET /api/ai/autocomplete/exercises?query=...`
- `GET /api/ai/autocomplete/meals?query=...`
- `POST /api/ai/clear-data`

### Motion Lab (JWT required)
- `POST /api/lab/sync`
- `GET /api/lab/search?q=...&channelId=...&limit=48`
- `GET /api/lab/channels`
- `GET /api/lab/status`

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
- `meal_time` (TIME, nullable)

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
- H2 console access (`/h2-console`) when running with profile `h2`
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
- Postgres via Docker Compose (default) or H2 via `-Dspring-boot.run.profiles=h2`
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
- If using Postgres: ensure Docker Compose is running and reachable at `localhost:5432`
- If using H2: start with profile `h2` and verify `/h2-console`
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
No `LICENSE` file is included in this repo. Add one if you intend to publish publicly.

## Support

For technical support or questions:
- Review application logs for error details
- Check API documentation for endpoint usage
- Verify configuration settings
- Review troubleshooting section above
