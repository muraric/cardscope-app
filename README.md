# CardScope

CardScope – Your smart credit card rewards advisor

## Overview

CardScope is a Spring Boot application that helps users optimize their credit card rewards by analyzing their spending patterns and suggesting the best credit cards for their needs. The application uses AI-powered analysis and Google Places integration to provide personalized recommendations.

## Features

- 🤖 AI-powered credit card suggestions based on spending patterns
- 📍 Google Places integration for nearby locations
- 💳 Credit card reward analysis and recommendations
- 👤 User profile management with card tracking
- 🔐 Secure authentication and password reset functionality
- 📧 Email notifications

## Tech Stack

- **Framework**: Spring Boot 3.5.6
- **Build Tool**: Gradle 8.5
- **Java Version**: 17
- **Database**: PostgreSQL (production), H2 (development)
- **Security**: Spring Security
- **AI**: OpenAI API
- **Email**: Spring Mail
- **Reactive**: WebFlux for Google Places API integration

## Prerequisites

- Java 17 or higher
- PostgreSQL (for production)
- Gradle 8.5 (included via wrapper - no installation needed)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd cardscope-app
```

### 2. Build the Project

```bash
# Windows
.\gradlew.bat build

# Linux/Mac
./gradlew build
```

### 3. Configure Application Properties

Create or update `src/main/resources/application.properties` with your configuration:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/cardscope
spring.datasource.username=your-username
spring.datasource.password=your-password

# OpenAI API
openai.api.key=your-openai-api-key

# Google Places API
google.places.api.key=your-google-places-api-key

# Application
app.baseUrl=https://your-domain.com
app.frontend.url=https://your-frontend-url.com

# Email (for password reset)
spring.mail.host=smtp.gmail.com
spring.mail.username=your-email@example.com
spring.mail.password=your-app-password
spring.mail.from=noreply@your-domain.com
```

### 4. Run the Application

```bash
# Windows
.\gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 5. Build Executable JAR

```bash
# Windows
.\gradlew.bat bootJar

# Linux/Mac
./gradlew bootJar
```

The JAR file will be created in `build/libs/`

## Running with Docker

### Build Docker Image

```bash
docker build -t cardscope-app .
```

### Run Docker Container

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/cardscope \
  -e SPRING_DATASOURCE_USERNAME=your-username \
  -e SPRING_DATASOURCE_PASSWORD=your-password \
  cardscope-app
```

## Project Structure

```
cardscope-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/shomuran/cardscope/
│   │   │       ├── config/          # Configuration classes
│   │   │       ├── controller/      # REST controllers
│   │   │       ├── dto/             # Data Transfer Objects
│   │   │       ├── model/           # JPA entities
│   │   │       ├── repository/      # JPA repositories
│   │   │       ├── service/         # Business logic
│   │   │       └── jobs/            # Scheduled jobs
│   │   └── resources/
│   │       ├── application.properties
│   │       └── prompts/            # AI prompt templates
│   └── test/
│       └── java/
│           └── com/shomuran/cardscope/
│               └── CardScopeApplicationTests.java
├── build.gradle                     # Gradle build configuration
├── settings.gradle                  # Gradle settings
├── gradle.properties               # Gradle properties
├── Dockerfile                       # Docker configuration
└── HELP.md                         # Additional documentation
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `POST /api/auth/forgot-password` - Request password reset
- `POST /api/auth/reset-password` - Reset password with token

### User Profile
- `GET /api/user/profile` - Get user profile
- `PUT /api/user/profile` - Update user profile
- `POST /api/user/cards` - Add credit card
- `DELETE /api/user/cards/{id}` - Remove credit card

### Credit Cards
- `GET /api/cards` - List all credit cards
- `GET /api/cards/{id}/rewards` - Get reward details for a card

### Suggestions
- `POST /api/suggestions` - Get credit card recommendations
- `POST /api/suggestions/json` - Get recommendations as JSON

### Places
- `POST /api/places/nearby-search` - Search nearby places
- `GET /api/places/{placeId}` - Get place details

### Health
- `GET /api/health` - Health check

## Development

### Run Tests

```bash
# Windows
.\gradlew.bat test

# Linux/Mac
./gradlew test
```

### Clean Build

```bash
# Windows
.\gradlew.bat clean build

# Linux/Mac
./gradlew clean build
```

### View Dependencies

```bash
# Windows
.\gradlew.bat dependencies

# Linux/Mac
./gradlew dependencies
```

## Contributing

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit a pull request

## License

[Add your license information here]

## Support

For issues and questions, please open an issue on the repository.

