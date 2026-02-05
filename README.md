# Places Tracker

A modern web application for tracking visited places (parks, restaurants, landmarks, etc.) with Google Maps integration, photo management, and visit history.

## Features

- **Place Management** - Add, edit, and organize places you've visited
- **Google Maps Integration** - Auto-fill place details from Google Maps URLs
- **Visit Tracking** - Record multiple visits per place with dates, temperatures, and notes
- **Photo Gallery** - Upload photos for each visit with lightbox viewing
- **Distance Calculation** - See driving distances from your home location
- **Google Reviews** - View ratings and reviews from Google
- **Search & Filter** - Find places by name, location, or country
- **Dark Mode** - Automatic theme based on system preferences

## Tech Stack

- **Backend**: Spring Boot 4.0, Java 25
- **Database**: MongoDB 8.0 with GridFS (photo storage)
- **Frontend**: Thymeleaf, htmx, Bootstrap 5.3
- **Build**: Gradle 9.2
- **Testing**: JUnit 6, Testcontainers

## Prerequisites

- **Java 25+** (for local development)
- **MongoDB 8.0+** (local or containerized)
- **Google Maps API Key** with these APIs enabled:
  - Places API (New)
  - Maps JavaScript API
  - Distance Matrix API

## Quick Start

### Option 1: Docker Compose (Recommended)

The fastest way to get started - runs both the app and MongoDB in containers.

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/places-tracker.git
   cd places-tracker
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and add your GOOGLE_MAPS_API_KEY
   ```

3. **Start the application**
   ```bash
   ./compose-up-standalone.sh
   ```

4. **Access the app**

   Open https://localhost:8443/placestracker in your browser.

   Accept the self-signed certificate warning (expected for local development).

5. **Stop the application**
   ```bash
   ./compose-down-standalone.sh
   ```

### Option 2: Local Development

Run the app locally with a containerized MongoDB.

1. **Clone and configure**
   ```bash
   git clone https://github.com/your-username/places-tracker.git
   cd places-tracker
   cp .env.example .env
   # Edit .env and add your GOOGLE_MAPS_API_KEY
   ```

2. **Start MongoDB**
   ```bash
   ./compose-up.sh
   ```

3. **Run the application**
   ```bash
   # Set your API key
   export GOOGLE_MAPS_API_KEY=your_api_key_here

   # Run with Gradle
   ./gradlew bootRun
   ```

4. **Access the app**

   Open https://localhost:8443/placestracker

5. **Stop MongoDB when done**
   ```bash
   ./compose-down.sh
   ```

### Option 3: Full Local Setup

Run everything locally without Docker.

1. **Install and start MongoDB**
   ```bash
   # macOS
   brew install mongodb-community@8.0
   brew services start mongodb-community@8.0

   # Ubuntu
   # Follow: https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-ubuntu/
   ```

2. **Clone and configure**
   ```bash
   git clone https://github.com/your-username/places-tracker.git
   cd places-tracker
   ```

3. **Run the application**
   ```bash
   export GOOGLE_MAPS_API_KEY=your_api_key_here
   ./gradlew bootRun
   ```

4. **Access the app**

   Open https://localhost:8443/placestracker

## Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `GOOGLE_MAPS_API_KEY` | Yes | - | Google Cloud API key |
| `SPRING_MONGODB_URI` | No | `mongodb://localhost:27017/placestracker` | MongoDB connection string |
| `CERT_PASSWORD` | No | `placestracker-dev-cert` | SSL certificate password |
| `CERT_HOSTS` | No | `localhost,placestracker.local` | SSL certificate hostnames |
| `JAVA_OPTS` | No | `-Xms512m -Xmx1g` | JVM memory settings |

### Google Maps API Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable these APIs:
   - **Places API (New)** - for place search and auto-fill
   - **Maps JavaScript API** - for embedded maps
   - **Distance Matrix API** - for driving distance calculations
4. Create an API key under **APIs & Services > Credentials**
5. (Recommended) Restrict the key to your domain/IP

### Application Properties

Key configuration in `src/main/resources/application.properties`:

```properties
# Server
server.port=8443
server.servlet.context-path=/placestracker

# Photo upload limits
app.photo.max-size-mb=15
spring.servlet.multipart.max-request-size=100MB

# MongoDB
spring.mongodb.uri=${SPRING_MONGODB_URI:mongodb://localhost:27017/placestracker}
```

## Development

### Running Tests

```bash
# Run all tests (uses Testcontainers - requires Docker)
./gradlew test

# Run with verbose output
./gradlew test --info
```

### Building

```bash
# Build JAR (skip tests for faster builds)
./gradlew bootJar -x test

# Build with tests
./gradlew build

# Output: build/libs/places-tracker-*.jar
```

### Hot Reload

Spring Boot DevTools is included. Changes to templates and code trigger automatic restart when running with `bootRun`.

### Code Structure

```
src/main/java/dk/placestracker/
├── PlacesTrackerApplication.java  # Main entry point
├── controller/                     # Web controllers
├── domain/                         # Place, Visit, Review records
├── repository/                     # MongoDB repositories
├── service/                        # Business logic
└── util/                           # Helper utilities

src/main/resources/
├── application.properties          # Configuration
├── templates/                      # Thymeleaf templates
└── static/                         # CSS, JS, images
```

## Docker

### Build Image Only

```bash
docker build -t placestracker:latest .
```

### Run with External MongoDB

```bash
docker run -d \
  --name placestracker \
  -p 8443:8443 \
  -e GOOGLE_MAPS_API_KEY=your_key \
  -e SPRING_MONGODB_URI=mongodb://host.docker.internal:27017/placestracker \
  placestracker:latest
```

### Docker Compose Files

| File | Description |
|------|-------------|
| `docker-compose.yml` | MongoDB only (for local development) |
| `docker-compose.standalone.yml` | Full stack (app + MongoDB) |

### Helper Scripts

| Script | Description |
|--------|-------------|
| `compose-up.sh` | Start MongoDB only |
| `compose-down.sh` | Stop MongoDB |
| `compose-up-standalone.sh` | Start full stack |
| `compose-down-standalone.sh` | Stop full stack |
| `build-and-run.sh` | Build and run locally |

## Usage Tips

### Adding a Place

1. Click **New Place**
2. Paste a Google Maps URL to auto-fill details, or enter manually
3. Add visit information (date, temperature, notes)
4. Upload photos for each visit
5. Save

### Google Maps URL Auto-fill

The app extracts place details from Google Maps URLs:
- Name, address, coordinates
- Ratings and reviews
- Place ID for linking back to Google

Just paste the URL in the Google Maps URL field and click **Extract**.

### Setting Home Location

1. Go to **Settings**
2. Enter your home coordinates
3. Distance from home will appear on place detail pages

## License

MIT License - see [LICENSE](LICENSE) for details.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [htmx](https://htmx.org/)
- [Bootstrap](https://getbootstrap.com/)
- [Lightbox2](https://lokeshdhakar.com/projects/lightbox2/)
