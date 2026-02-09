# Places Tracker: Design and Implementation Guide

**Version 1.2.0-SNAPSHOT**
**Author: Dmitriy Kopylenko**

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack and Build Configuration](#2-technology-stack-and-build-configuration)
3. [Architecture Overview](#3-architecture-overview)
4. [Project Structure](#4-project-structure)
5. [Domain Model — Detailed Code Walkthrough](#5-domain-model--detailed-code-walkthrough)
6. [Repository Layer — Detailed Code Walkthrough](#6-repository-layer--detailed-code-walkthrough)
7. [Service Layer — Detailed Code Walkthrough](#7-service-layer--detailed-code-walkthrough)
8. [Web Layer (Controllers) — Detailed Code Walkthrough](#8-web-layer-controllers--detailed-code-walkthrough)
9. [DTO Layer — Detailed Code Walkthrough](#9-dto-layer--detailed-code-walkthrough)
10. [Utility Classes — Detailed Code Walkthrough](#10-utility-classes--detailed-code-walkthrough)
11. [Configuration and Startup — Detailed Code Walkthrough](#11-configuration-and-startup--detailed-code-walkthrough)
12. [Frontend Architecture — Detailed Code Walkthrough](#12-frontend-architecture--detailed-code-walkthrough)
13. [Testing Strategy — Detailed Code Walkthrough](#13-testing-strategy--detailed-code-walkthrough)
14. [Deployment — Detailed Code Walkthrough](#14-deployment--detailed-code-walkthrough)

---

## 1. Project Overview

Places Tracker is a full-stack web application for documenting journeys to memorable places. Users can record visited locations, log individual visits with photos, temperatures, and notes, manage a wishlist of places to visit, mark favorites, and view driving distances from a configured home location.

### Core Features

- **Place Management**: CRUD operations for places with name, location, state, country, coordinates, and Google Maps metadata
- **Visit Tracking**: Multiple visits per place with date, temperature (F/C), duration, notes, and photo attachments
- **Wishlist**: Track places you want to visit; convert wishlist items to visited with one action
- **Favorites**: Star places for quick access
- **Google Maps Auto-fill**: Paste a Google Maps URL to auto-populate place details, rating, and reviews
- **Photo Management**: Upload, thumbnail generation, lightbox viewing per visit via MongoDB GridFS
- **Distance Calculation**: Driving distances from home via Google Distance Matrix API with Haversine fallback
- **Sortable/Paginated Lists**: Server-side sort by name, distance, or last visit with configurable page sizes
- **Real-time Search**: htmx-powered live search across all list views
- **Reference Data**: Pre-loaded North American countries and states/provinces

---

## 2. Technology Stack and Build Configuration

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 25 |
| Framework | Spring Boot | 4.0.2 |
| Database | MongoDB | 8.0 |
| Template Engine | Thymeleaf + Layout Dialect | |
| Frontend Interactivity | htmx | 2.0.3 |
| CSS Framework | Bootstrap | 5.3.3 |
| Icons | Bootstrap Icons | 1.11.3 |
| Image Lightbox | Lightbox2 | 2.11.4 |
| Image Thumbnails | Thumbnailator | 0.4.20 |
| JSON Processing | Jackson (tools.jackson) | |
| HTTP Client | Spring RestClient | |
| File Storage | MongoDB GridFS | |
| Build Tool | Gradle | |
| Testing | JUnit 6, Testcontainers, MockMvc | |

### 2.1 build.gradle — Line-by-Line

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.2'   // Spring Boot Gradle plugin — provides bootJar, bootRun tasks
    id 'io.spring.dependency-management' version '1.1.6'  // Manages transitive dependency versions via BOMs
}

group = 'dk'                    // Maven-style group ID for the project
version = '1.2.0-SNAPSHOT'     // Current development version

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)  // Forces Gradle to use Java 25 JDK
    }
}
```

**Dependencies explained:**

```groovy
dependencies {
    // Core web starter: embedded Tomcat, Spring MVC, Jackson for JSON
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Spring RestClient support for outbound HTTP calls (Google APIs)
    implementation 'org.springframework.boot:spring-boot-starter-restclient'

    // MongoDB driver + Spring Data MongoDB (repositories, GridFS template)
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'

    // Thymeleaf template engine for server-side HTML rendering
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'

    // Jakarta Validation API support (@NotBlank, @Size, @DecimalMin, etc.)
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Thymeleaf Layout Dialect: adds layout:decorate / layout:fragment for template inheritance
    implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect'

    // Jackson databind for manual JSON parsing (Google reviews JSON from frontend)
    implementation 'com.fasterxml.jackson.core:jackson-databind'

    // Thumbnailator: generates thumbnails from uploaded photos (resizes, compresses, converts format)
    implementation 'net.coobird:thumbnailator:0.4.20'

    // Spring DevTools: automatic restart on code changes during development
    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    // Test starters — includes JUnit 6, AssertJ, Mockito, Spring Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'    // MockMvc
    testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-test'  // Embedded MongoDB testing

    // Testcontainers for spinning up real MongoDB 8.0 in Docker during tests
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation platform('org.testcontainers:testcontainers-bom:1.20.4')  // BOM for version alignment
    testImplementation 'org.testcontainers:mongodb'       // MongoDB-specific Testcontainers module
    testImplementation 'org.testcontainers:junit-jupiter'  // JUnit Jupiter integration
}
```

### 2.2 application.properties — Line-by-Line

```properties
# --- Application Identity ---
spring.application.name=places-tracker      # Name shown in actuator/logs
server.port=8443                             # HTTPS port (not default 8080)
server.servlet.context-path=/placestracker   # All URLs prefixed with /placestracker

# --- SSL Configuration ---
# Self-signed PKCS12 keystore bundled in classpath for local development.
# In Docker, an auto-generated keystore at /app/certs is used instead.
server.ssl.enabled=true
server.ssl.key-store=classpath:ssl/keystore.p12   # Keystore file location
server.ssl.key-store-password=changeit             # Keystore password
server.ssl.key-store-type=PKCS12                   # PKCS12 format (not JKS)
server.ssl.key-alias=placestracker                 # Alias of the key pair in the keystore

# --- MongoDB Connection ---
# Spring Boot 4.0 changed the property prefix from spring.data.mongodb.* to spring.mongodb.*
# The ${SPRING_MONGODB_URI:...} syntax uses the environment variable if set, falls back to localhost.
spring.mongodb.uri=${SPRING_MONGODB_URI:mongodb://localhost:27017/placestracker}
spring.data.mongodb.auto-index-creation=true   # Auto-create indexes from @Indexed annotations

# --- Thymeleaf ---
spring.thymeleaf.cache=false          # Disable template caching during development (enable in prod)
spring.thymeleaf.prefix=classpath:/templates/   # Template directory
spring.thymeleaf.suffix=.html                   # File extension

# --- File Upload Limits ---
app.photo.max-size-mb=15                                          # Custom property: max photo size
spring.servlet.multipart.max-file-size=${app.photo.max-size-mb}MB  # Spring Boot multipart limit per file
spring.servlet.multipart.max-request-size=100MB                    # Total request size (multiple photos)
server.tomcat.max-part-count=1000    # Tomcat 11+ enforces strict part limits; 1000 covers forms with many fields

# --- Logging ---
logging.level.dk.placestracker=DEBUG                         # App-level debug logging
logging.level.org.springframework.data.mongodb=DEBUG          # MongoDB query logging
logging.file.name=logs/places-tracker.log                     # Log to file
logging.file.max-size=10MB                                    # Rotate at 10MB
logging.file.max-history=7                                    # Keep 7 rotated files

# --- Date Formatting ---
spring.mvc.format.date=yyyy-MM-dd   # How Spring MVC parses date parameters from forms

# --- Google Maps API ---
google.maps.api-key=${GOOGLE_MAPS_API_KEY:}          # API key from env var (empty string if unset)
google.maps.timeout.seconds=10                        # HTTP timeout for API calls
google.maps.distance-matrix.batch-size=25             # Max destinations per Distance Matrix API call
```

---

## 3. Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│                    Web Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │HomeController│  │PlaceController│  │Settings    │ │
│  │              │  │(1110 lines)   │  │Controller  │ │
│  └──────────────┘  └──────────────┘  └────────────┘ │
├──────────────────────────────────────────────────────┤
│                   Service Layer                       │
│  ┌──────────┐ ┌──────────┐ ┌────────┐ ┌───────────┐ │
│  │PlaceServ.│ │DistServ. │ │PhotoSv.│ │GoogleMaps │ │
│  │Settings  │ │RefData   │ │        │ │Service    │ │
│  └──────────┘ └──────────┘ └────────┘ └───────────┘ │
├──────────────────────────────────────────────────────┤
│                 Repository Layer                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│  │PlaceRepo │ │Settings  │ │Country   │ │StatePr.│ │
│  │(MongoDB) │ │Repo      │ │Repo      │ │Repo    │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘ │
├──────────────────────────────────────────────────────┤
│              MongoDB (+ GridFS for photos)            │
└──────────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **Java Records for Domain Model**: All domain entities are immutable Java records with "wither" methods for state transitions. This prevents accidental mutation and makes data flow explicit — every change produces a new object.

2. **htmx for Interactivity**: Instead of a SPA framework (React, Vue), the application uses htmx for dynamic content updates. The server renders HTML fragments and htmx swaps them into the DOM. This keeps the frontend logic thin and the server authoritative.

3. **MongoDB GridFS for Photos**: Photos and auto-generated thumbnails are stored in MongoDB GridFS with visit-level metadata, enabling cascade deletes and batch existence checks without a separate file storage system.

4. **Haversine Fallback**: When the Google Distance Matrix API is unavailable or unconfigured, distances are calculated using the Haversine formula (great-circle approximation). The UI labels these as "approx".

5. **Backward-Compatible Status Field**: The `status` field on Place uses `null` or `"VISITED"` for backward compatibility with pre-existing documents, and `"TO_VISIT"` for wishlist items. Queries use `$or: [{status: null}, {status: 'VISITED'}]`.

---

## 4. Project Structure

```
src/main/java/dk/placestracker/
├── PlacesTrackerApplication.java          # Spring Boot entry point (@SpringBootApplication)
├── config/
│   ├── MongoConfig.java                   # @EnableMongoRepositories + @EnableMongoAuditing
│   └── ReferenceDataLoader.java           # ApplicationRunner: loads countries/states on startup
├── domain/
│   ├── model/
│   │   ├── Place.java                     # Core entity (22 fields, wither methods, status helpers)
│   │   ├── Visit.java                     # Embedded visit record (UUID id, photos, temperature)
│   │   ├── Review.java                    # Embedded Google review record
│   │   ├── Settings.java                  # Singleton settings (home location)
│   │   ├── Country.java                   # Reference data record
│   │   └── StateProvince.java             # Reference data record with compound index
│   └── repository/
│       ├── PlaceRepository.java           # MongoRepository with 15 custom queries
│       ├── SettingsRepository.java
│       ├── CountryRepository.java
│       └── StateProvinceRepository.java
├── service/
│   ├── PlaceService.java                  # Interface (20 methods)
│   ├── DefaultPlaceService.java           # CRUD, search, favorites, wishlist (273 lines)
│   ├── DistanceService.java               # Interface
│   ├── DefaultDistanceService.java        # Google API + cache + Haversine (280 lines)
│   ├── DistanceResult.java                # Result record with Source enum
│   ├── PhotoService.java                  # Interface with PhotoData inner record
│   ├── DefaultPhotoService.java           # GridFS storage, thumbnails (363 lines)
│   ├── GoogleMapsService.java             # Interface
│   ├── DefaultGoogleMapsService.java      # Places API (New) integration (427 lines)
│   ├── SettingsService.java               # Interface
│   ├── DefaultSettingsService.java        # Settings + distance invalidation (56 lines)
│   ├── ReferenceDataService.java          # Interface
│   └── DefaultReferenceDataService.java   # Countries/states lookups
├── util/
│   ├── DistanceCalculator.java            # Haversine formula + formatting (113 lines)
│   └── DurationUtils.java                 # Human-friendly duration parsing (121 lines)
└── web/
    ├── controller/
    │   ├── HomeController.java            # Dashboard (41 lines)
    │   ├── PlaceController.java           # Main CRUD + photos + search (1110 lines)
    │   └── SettingsController.java        # Home location settings (88 lines)
    └── dto/
        ├── PlacePage.java                 # In-memory pagination DTO (67 lines)
        ├── PlaceExtractResponse.java      # Google URL extract response (43 lines)
        └── DistanceMatrixResponse.java    # Distance Matrix API response (92 lines)
```

---

## 5. Domain Model — Detailed Code Walkthrough

### 5.1 Place.java (321 lines) — The Central Entity

The `Place` record is the heart of the application. It is stored in the `places` MongoDB collection and uses Java 25's record feature for immutability.

#### Record Declaration and Fields (lines 22–90)

```java
@Document(collection = "places")            // Maps to MongoDB "places" collection
public record Place(
    @Id String id,                          // MongoDB auto-generated ObjectId (as String)

    @NotBlank(message = "Place name is required")
    @Size(min = 2, max = 200, message = "Place name must be between 2 and 200 characters")
    @Indexed                                // MongoDB index for faster name lookups
    String name,

    @NotBlank(message = "Location is required")
    @Size(max = 200) String location,       // City or locality (e.g., "Yosemite Valley")

    @NotBlank(message = "State is required")
    @Size(max = 100) @Indexed String state, // State code or name, indexed for filtering

    @NotBlank(message = "Country is required")
    @Size(max = 100) String country,        // Country code ("US", "CA", "MX")

    List<Visit> visits,                     // Embedded visit documents (not a separate collection)

    Boolean hasToilet,                      // IMPORTANT: Boolean wrapper, NOT primitive boolean
                                            // Null from MongoDB → compact constructor defaults to false

    Double latitude,                        // GPS coordinates for distance calculations
    Double longitude,                       // Both nullable (not all places have coordinates)

    // --- Google Maps extracted data (auto-filled from URL) ---
    String formattedAddress,                // Full Google-formatted address
    String googlePlaceId,                   // Google's unique place identifier
    String website,                         // Official website URL
    String phoneNumber,                     // Contact phone number
    Double googleRating,                    // Star rating 1.0–5.0 (null if no rating)
    Integer googleReviewCount,              // Total number of Google reviews
    List<Review> googleReviews,             // Up to 5 embedded review documents

    LocalDateTime createdAt,                // Set once on creation, never changed
    LocalDateTime updatedAt,                // Updated on every save

    // --- Driving distance cache ---
    Double drivingDistanceMiles,            // Cached Google API driving distance
    Double drivingDurationMinutes,          // Cached Google API driving duration
    LocalDateTime distanceCalculatedAt,     // When the cache was last updated
    String distanceFromHomeLatLng,          // "lat,lng" snapshot of home when calculated
                                            // If home moves, cache is invalidated

    // --- Favorites & Wishlist ---
    Boolean favorite,                       // IMPORTANT: Boolean wrapper (same reason as hasToilet)
    String status                           // "VISITED", "TO_VISIT", or null (backward compat)
) {
```

**Why `Boolean` instead of `boolean`?** This is a critical design decision. When MongoDB deserializes a document that was created before the `favorite` field existed, the field is `null`. If the record used primitive `boolean`, this would throw a `NullPointerException`. Similarly, Spring MVC form binding sends no value for unchecked checkboxes, resulting in `null`. The compact constructor handles this:

#### Compact Constructor (lines 92–95)

```java
    public Place {
        hasToilet = hasToilet != null ? hasToilet : false;   // Default null → false
        favorite = favorite != null ? favorite : false;       // Default null → false
    }
```

This runs after the canonical constructor, patching `null` values to `false`. It handles both MongoDB backward compatibility and form binding.

#### Factory Method: `create()` (lines 98–110)

```java
    public static Place create(String name, String location, String state, String country,
                              List<Visit> visits, boolean hasToilet, Double latitude, Double longitude,
                              String formattedAddress, String googlePlaceId, String website, String phoneNumber,
                              Double googleRating, Integer googleReviewCount, List<Review> googleReviews) {
        return new Place(
            null,                                      // id: null — MongoDB will auto-generate
            name, location, state, country,
            visits != null ? visits : new ArrayList<>(),  // Defensive: never store null lists
            hasToilet,
            latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
            googleRating, googleReviewCount,
            googleReviews != null ? googleReviews : new ArrayList<>(),
            null, null,                                // createdAt, updatedAt: set by service layer
            null, null, null, null,                    // distance cache: empty initially
            false,                                     // favorite: not a favorite by default
            "VISITED"                                  // status: visited (not wishlist)
        );
    }
```

The factory method enforces invariants: visits and reviews are never `null` (always at least empty lists), status is always `"VISITED"`, and favorite is `false`.

#### Factory Method: `createWishlistItem()` (lines 113–125)

```java
    public static Place createWishlistItem(String name, String location, String state, String country,
                                           Double latitude, Double longitude, ...) {
        return new Place(null, name, location, state, country,
            new ArrayList<>(),       // Wishlist items start with no visits
            false,                   // No facilities info for wishlist
            latitude, longitude, ...,
            false,                   // Not a favorite by default
            "TO_VISIT");             // KEY DIFFERENCE: status is "TO_VISIT"
    }
```

The only difference from `create()`: `status = "TO_VISIT"` and empty visits list. This is how the application distinguishes visited places from wishlist items.

#### Wither Methods (lines 128–310)

Since records are immutable, modifications produce new instances. Each "wither" method creates a new `Place` with one aspect changed while preserving all other fields:

**`withTimestamps()`** — Sets `createdAt` and `updatedAt`. Used when first creating a place.

**`withUpdate()`** — The most important wither. Accepts all editable fields plus a new `updatedAt`. Crucially, it **preserves** `createdAt` from the existing instance and **preserves** the distance cache fields. However, it does **NOT** carry `favorite` or `status` — these must be chained separately:

```java
    // In DefaultPlaceService.update():
    Place updatedPlace = existingPlace.withUpdate(..., LocalDateTime.now())
        .withFavorite(place.favorite())    // Must chain — withUpdate doesn't carry these
        .withStatus(place.status());       // Must chain — withUpdate doesn't carry these
```

**Without this chain, edits would silently reset `favorite` to `false` and `status` to whatever was on the existing place.** This is a documented pitfall.

**`withAddedVisit(Visit visit)`** — Creates a copy with the visit appended, then **sorts visits by date descending** (most recent first). The sort ensures consistent display order:

```java
    public Place withAddedVisit(Visit visit) {
        List<Visit> updatedVisits = new ArrayList<>(visits != null ? visits : new ArrayList<>());
        updatedVisits.add(visit);
        updatedVisits.sort(Comparator.comparing(Visit::date).reversed());  // Most recent first
        return new Place(id, name, ..., updatedVisits, ...);
    }
```

**`withRemovedVisit(String visitId)`** — Removes a visit by ID. Does **not** cascade-delete photos — that responsibility belongs to the service layer.

**`withUpdatedVisit(String visitId, Visit updatedVisit)`** — Replaces a visit in the list by matching ID, then re-sorts.

**`withDrivingDistance(miles, durationMinutes, homeLatLng)`** — Caches driving distance data along with a snapshot of the home location. The snapshot is key: if the user later changes their home location, all cached distances become invalid because `distanceFromHomeLatLng` won't match the new home.

**`withClearedDrivingDistance()`** — Sets all four distance cache fields to `null`. Called when home location changes.

**`hasValidCachedDistance(String currentHomeLatLng)`** — Returns `true` only if (a) there is a cached distance and (b) the home location snapshot matches the current home:

```java
    public boolean hasValidCachedDistance(String currentHomeLatLng) {
        if (drivingDistanceMiles == null || distanceFromHomeLatLng == null) return false;
        return distanceFromHomeLatLng.equals(currentHomeLatLng);
    }
```

#### Status Helpers (lines 314–320)

```java
    public boolean isVisited() {
        return status == null || "VISITED".equals(status);   // null = pre-existing (visited)
    }

    public boolean isWishlist() {
        return "TO_VISIT".equals(status);
    }
```

The `null` check in `isVisited()` provides backward compatibility: documents created before the `status` field was added have `null` status, but they are visited places.

### 5.2 Visit.java (111 lines) — Embedded Visit Record

Visits are embedded within the Place document, not stored in a separate collection.

```java
public record Visit(
    @NotNull String id,                      // UUID string — NOT a MongoDB ObjectId
    @NotNull LocalDate date,                 // Visit date (not datetime — just the day)
    @DecimalMin("-50.0") @DecimalMax("150.0")
    Double temperatureF,                     // Optional: temperature in Fahrenheit
    @Size(max = 2000) String notes,          // Optional: free-text notes
    Duration duration,                       // Optional: java.time.Duration (e.g., 1h25min)
    List<String> photoIds                    // GridFS file IDs (not the photos themselves)
) {
```

**Why UUID for visit IDs?** Visits are embedded, so MongoDB doesn't auto-generate IDs for them. The application generates UUIDs via the `create()` factory:

```java
    public static Visit create(LocalDate date, Double temperatureF, String notes, Duration duration) {
        return new Visit(
            UUID.randomUUID().toString(),    // Generate unique ID
            date, temperatureF, notes, duration,
            new ArrayList<>()                // Start with no photos
        );
    }
```

**Temperature conversion:**

```java
    public Double getTemperatureC() {
        if (temperatureF == null) return null;  // Null-safe
        return (temperatureF - 32) * 5.0 / 9.0;  // Standard F→C formula
    }
```

**Photo management withers** follow the same immutability pattern as Place. Each returns a new Visit:
- `withAddedPhoto(photoId)` — Copies the photo list, appends the new ID
- `withRemovedPhoto(photoId)` — Copies the photo list, removes the ID
- `withPhotos(newPhotoIds)` — Replaces the entire photo list (used during cleanup)

All three handle `null` photoIds defensively: `new ArrayList<>(photoIds != null ? photoIds : new ArrayList<>())`.

### 5.3 Review.java (26 lines) — Embedded Google Review

A simple record for Google reviews extracted via the Places API:

```java
public record Review(
    String authorName,              // Reviewer's display name
    Integer rating,                 // 1–5 star rating
    String text,                    // Review body text
    String relativeTime,            // e.g., "2 months ago" (from Google)
    String profilePhotoUrl,         // URL to the reviewer's profile photo
    LocalDateTime publishedAt       // Exact publication timestamp
) {
    public static Review create(...) { return new Review(...); }
}
```

### 5.4 Settings.java (60 lines) — Singleton Settings

The application uses a **singleton document pattern**: there is exactly one Settings document with `id = "default"`.

```java
@Document(collection = "settings")
public record Settings(
    @Id String id,
    @DecimalMin("-90.0") @DecimalMax("90.0") Double homeLatitude,     // Valid latitude range
    @DecimalMin("-180.0") @DecimalMax("180.0") Double homeLongitude,  // Valid longitude range
    LocalDateTime updatedAt
) {
    private static final String DEFAULT_ID = "default";

    public static Settings createDefault() {
        return new Settings(DEFAULT_ID, null, null, LocalDateTime.now());  // No home location initially
    }

    public boolean hasHomeLocation() {
        return homeLatitude != null && homeLongitude != null;  // Both must be set
    }

    public Settings withHomeLocation(Double latitude, Double longitude) {
        return new Settings(this.id, latitude, longitude, LocalDateTime.now());
    }

    public Settings withClearedHomeLocation() {
        return new Settings(this.id, null, null, LocalDateTime.now());
    }
}
```

### 5.5 Country.java and StateProvince.java — Reference Data

```java
@Document(collection = "countries")
public record Country(
    @Id String id,
    @Indexed(unique = true) String code,   // "US", "CA", "MX" — unique index prevents duplicates
    String name,                           // "United States", "Canada", "Mexico"
    Integer displayOrder                   // Controls dropdown sort order
) {
    public static Country create(String code, String name, Integer displayOrder) {
        return new Country(null, code, name, displayOrder);  // null id → MongoDB generates
    }
}

@Document(collection = "states_provinces")
@CompoundIndex(name = "country_code_idx", def = "{'countryCode': 1, 'code': 1}", unique = true)
// ↑ Compound unique index: no two states can have the same code within the same country
public record StateProvince(
    @Id String id,
    String code,                           // "CA", "NY", "AB", "QC", etc.
    String name,                           // "California", "Alberta", etc.
    @Indexed String countryCode,           // "US", "CA", "MX" — indexed for filtering
    Integer displayOrder
) { ... }
```

---

## 6. Repository Layer — Detailed Code Walkthrough

### 6.1 PlaceRepository.java (91 lines)

Extends `MongoRepository<Place, String>` which provides basic CRUD. Custom methods use both Spring Data derived queries and `@Query` annotations.

#### Spring Data Derived Queries

```java
List<Place> findByStateIgnoreCase(String state);
// MongoDB query: { state: { $regex: "^California$", $options: "i" } }
// The "IgnoreCase" suffix makes the match case-insensitive

List<Place> findByCountryIgnoreCase(String country);
List<Place> findByNameContainingIgnoreCase(String name);
// "Containing" adds $regex wildcards: { name: { $regex: ".*Yosemite.*", $options: "i" } }

List<Place> findByStateIgnoreCaseAndCountryIgnoreCase(String state, String country);
// Combines two conditions with AND

List<Place> findAllByOrderByNameAsc();
// Simple ORDER BY equivalent: sorts all documents by name ascending

List<Place> findByStatus(String status);
// Exact match: { status: "TO_VISIT" }

List<Place> findByFavoriteTrue();
// Boolean match: { favorite: true }

long countByStateIgnoreCase(String state);
long countByCountryIgnoreCase(String country);
long countByFavoriteTrue();
```

#### Custom @Query Methods

These handle the backward-compatible status field where `null` means "VISITED":

```java
    // All visited places: status is null (pre-existing docs) OR explicitly "VISITED"
    @Query("{ $or: [ { 'status': null }, { 'status': 'VISITED' } ] }")
    List<Place> findAllVisitedPlaces();

    // Favorite visited places: must be favorite AND visited
    @Query("{ 'favorite': true, $or: [ { 'status': null }, { 'status': 'VISITED' } ] }")
    List<Place> findFavoriteVisitedPlaces();

    // Search visited places: combines status filter with text search across 3 fields
    @Query("{ $and: [ " +
           "{ $or: [ { 'status': null }, { 'status': 'VISITED' } ] }, " +   // Must be visited
           "{ $or: [ " +
           "  { 'name': { $regex: ?0, $options: 'i' } }, " +                // Match name
           "  { 'location': { $regex: ?0, $options: 'i' } }, " +            // OR match location
           "  { 'state': { $regex: ?0, $options: 'i' } } " +                // OR match state
           "] } ] }")
    List<Place> searchVisitedPlaces(String searchTerm);
    // ?0 is the first method parameter (searchTerm), inserted into the query

    // Same pattern but filtered to wishlist items only
    @Query("{ $and: [ { 'status': 'TO_VISIT' }, { $or: [ ... ] } ] }")
    List<Place> searchWishlistPlaces(String searchTerm);

    // Count query (returns long, not List<Place>)
    @Query(value = "{ 'status': 'TO_VISIT' }", count = true)
    long countWishlistPlaces();
```

---

## 7. Service Layer — Detailed Code Walkthrough

### 7.1 DefaultPlaceService.java (273 lines)

The central service handling all place operations. Annotated with `@Service` and `@Transactional` at the class level.

#### Constructor Injection

```java
@Service
@Transactional                           // All methods are transactional by default
public class DefaultPlaceService implements PlaceService {
    private final PlaceRepository placeRepository;
    private final PhotoService photoService;    // Needed for cascade photo deletion

    public DefaultPlaceService(PlaceRepository placeRepository, PhotoService photoService) {
        this.placeRepository = placeRepository;
        this.photoService = photoService;
    }
```

#### `create()` — Creating a New Place (lines 69–74)

```java
    @Override
    public Place create(Place place) {
        LocalDateTime now = LocalDateTime.now();
        Place placeWithTimestamps = place.withTimestamps(now, now);  // Set BOTH createdAt and updatedAt
        return placeRepository.save(placeWithTimestamps);            // MongoDB generates id on save
    }
```

#### `update()` — The Most Complex Method (lines 77–133)

This method handles multiple concerns: visit ID generation, cascade photo deletion, and field preservation.

```java
    @Override
    public Place update(String id, Place place) {
        return placeRepository.findById(id)
            .map(existingPlace -> {
                // Step 1: Ensure all visits have IDs.
                // When a user adds a new visit on the edit page, it arrives with a blank ID.
                // We detect this and generate a UUID for it.
                List<Visit> visitsWithIds = place.visits().stream()
                    .map(visit -> {
                        if (visit.id() == null || visit.id().isBlank()) {
                            // New visit: generate ID, preserve any photos
                            return Visit.create(visit.date(), visit.temperatureF(),
                                              visit.notes(), visit.duration())
                                .withPhotos(visit.photoIds() != null ? visit.photoIds() : new ArrayList<>());
                        }
                        return visit;  // Existing visit: keep as-is
                    }).toList();

                // Step 2: Detect removed visits by comparing old and new visit ID lists.
                List<String> oldVisitIds = existingPlace.visits().stream()
                    .map(Visit::id).toList();
                List<String> newVisitIds = visitsWithIds.stream()
                    .map(Visit::id).toList();

                // Any old visit ID not in the new list was removed → cascade delete its photos
                oldVisitIds.stream()
                    .filter(visitId -> !newVisitIds.contains(visitId))
                    .forEach(photoService::deletePhotosByVisitId);

                // Step 3: Build the updated place.
                // withUpdate() preserves createdAt and distance cache.
                // CRITICAL: Chain .withFavorite() and .withStatus() because withUpdate() doesn't carry them.
                Place updatedPlace = existingPlace.withUpdate(
                    place.name(), place.location(), place.state(), place.country(),
                    visitsWithIds, place.hasToilet(),
                    place.latitude(), place.longitude(),
                    place.formattedAddress(), place.googlePlaceId(), place.website(), place.phoneNumber(),
                    place.googleRating(), place.googleReviewCount(),
                    LocalDateTime.now()
                ).withFavorite(place.favorite()).withStatus(place.status());

                return placeRepository.save(updatedPlace);
            })
            .orElseThrow(() -> new IllegalArgumentException("Place not found with id: " + id));
    }
```

#### `deleteById()` — Cascade Photo Deletion (lines 136–148)

```java
    @Override
    public void deleteById(String id) {
        Place place = placeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Place not found with id: " + id));

        // Before deleting the place, cascade delete ALL photos for ALL visits
        if (place.visits() != null) {
            place.visits().forEach(visit -> photoService.deletePhotosByVisitId(visit.id()));
        }

        placeRepository.deleteById(id);
    }
```

#### `toggleFavorite()` — Simple Toggle (lines 225–232)

```java
    @Override
    public Place toggleFavorite(String id) {
        return placeRepository.findById(id)
            .map(place -> {
                Place toggled = place.withFavorite(!place.favorite());  // Flip the boolean
                return placeRepository.save(toggled);
            })
            .orElseThrow(() -> new IllegalArgumentException("Place not found with id: " + id));
    }
```

#### `convertToVisited()` — Wishlist → Visited (lines 235–242)

```java
    @Override
    public Place convertToVisited(String id) {
        return placeRepository.findById(id)
            .map(place -> placeRepository.save(place.withStatus("VISITED")))
            .orElseThrow();
    }
```

#### Sorting Methods — Visit-Date Sorting (lines 38–54)

The `findAllByMostRecentVisit()` method sorts places by their most recent visit date. Places with no visits go to the end:

```java
    places.sort((p1, p2) -> {
        var v1 = p1.getMostRecentVisit();    // Optional<Visit>
        var v2 = p2.getMostRecentVisit();

        if (v1.isEmpty() && v2.isEmpty()) return 0;   // Both have no visits: equal
        if (v1.isEmpty()) return 1;                     // p1 has no visits: goes after p2
        if (v2.isEmpty()) return -1;                    // p2 has no visits: goes after p1

        return v2.get().date().compareTo(v1.get().date());  // Descending (most recent first)
    });
```

### 7.2 DefaultDistanceService.java (280 lines)

Implements a three-tier distance calculation strategy: **Cache → Google API → Haversine fallback**.

#### Constructor — API Key Detection (lines 39–56)

```java
    public DefaultDistanceService(
            SettingsService settingsService,
            PlaceRepository placeRepository,
            RestClient.Builder restClientBuilder,         // Spring's builder for HTTP clients
            @Value("${google.maps.api-key}") String apiKey,
            @Value("${google.maps.distance-matrix.batch-size:25}") int batchSize) {

        this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();  // Detect if API is available
        this.batchSize = batchSize;                                   // Default: 25 destinations/request

        if (!apiKeyConfigured) {
            log.warn("Google Maps API key not configured. Driving distances will use Haversine fallback.");
        }
    }
```

#### `getDistances()` — Batch Processing (lines 124–174)

The main method that processes a list of places:

```java
    @Override
    public Map<String, DistanceResult> getDistances(List<Place> places) {
        Map<String, DistanceResult> results = new HashMap<>();
        Settings settings = settingsService.getSettings();

        // 1. If no home location configured, everything is unavailable
        if (!settings.hasHomeLocation()) {
            places.forEach(p -> results.put(p.id(), DistanceResult.unavailable()));
            return results;
        }

        String homeLatLng = formatLatLng(settings.homeLatitude(), settings.homeLongitude());
        List<Place> needApiCall = new ArrayList<>();

        // 2. Triage each place into: no-coords, cached, or needs-api
        for (Place place : places) {
            if (place.latitude() == null || place.longitude() == null) {
                results.put(place.id(), DistanceResult.unavailable());  // No coordinates
            } else if (place.hasValidCachedDistance(homeLatLng)) {
                results.put(place.id(), DistanceResult.cached(           // Use cache
                    place.drivingDistanceMiles(), place.drivingDurationMinutes()));
            } else {
                needApiCall.add(place);                                  // Needs API call
            }
        }

        // 3. If no API key, use Haversine for all remaining
        if (!apiKeyConfigured) {
            for (Place place : needApiCall) {
                results.put(place.id(), calculateFallbackDistance(place, settings));
            }
            return results;
        }

        // 4. Batch API calls (max 25 destinations per request)
        List<List<Place>> batches = partition(needApiCall, batchSize);
        for (List<Place> batch : batches) {
            processBatch(batch, homeLatLng, settings, results);
        }
        return results;
    }
```

#### `processBatch()` — API Call with Fallback (lines 194–241)

```java
    private void processBatch(List<Place> batch, String homeLatLng, Settings settings,
                              Map<String, DistanceResult> results) {
        // Build pipe-separated destinations: "37.7,-119.6|44.4,-110.6|36.1,-112.1"
        String destinations = batch.stream()
            .map(p -> formatLatLng(p.latitude(), p.longitude()))
            .collect(Collectors.joining("|"));

        try {
            DistanceMatrixResponse response = callDistanceMatrixApi(homeLatLng, destinations);

            if (response != null && response.isOk()) {
                for (int i = 0; i < batch.size(); i++) {
                    Place place = batch.get(i);
                    DistanceMatrixResponse.Element element = response.getElement(i);

                    if (element != null && element.isOk()) {
                        double miles = element.distance().toMiles();      // meters → miles
                        double minutes = element.duration().toMinutes();  // seconds → minutes

                        // Cache the result on the Place entity for future requests
                        Place updatedPlace = place.withDrivingDistance(miles, minutes, homeLatLng);
                        placeRepository.save(updatedPlace);

                        results.put(place.id(), DistanceResult.fromApi(miles, minutes));
                    } else {
                        // This specific destination failed — fall back to Haversine
                        results.put(place.id(), calculateFallbackDistance(place, settings));
                    }
                }
            } else {
                // Entire batch failed — fall back to Haversine for all
                for (Place place : batch) {
                    results.put(place.id(), calculateFallbackDistance(place, settings));
                }
            }
        } catch (Exception e) {
            // Network error, timeout, etc. — fall back for entire batch
            for (Place place : batch) {
                results.put(place.id(), calculateFallbackDistance(place, settings));
            }
        }
    }
```

#### `invalidateAllDistances()` — Called When Home Moves (lines 177–192)

```java
    @Override
    public void invalidateAllDistances() {
        List<Place> allPlaces = placeRepository.findAll();
        int cleared = 0;
        for (Place place : allPlaces) {
            if (place.drivingDistanceMiles() != null) {
                Place clearedPlace = place.withClearedDrivingDistance();
                placeRepository.save(clearedPlace);
                cleared++;
            }
        }
        log.info("Cleared driving distance cache for {} places", cleared);
    }
```

### 7.3 DefaultPhotoService.java (363 lines)

Manages photo storage in MongoDB GridFS.

#### Photo Upload (lines 61–83)

```java
    @Override
    public String savePhotoForVisit(MultipartFile file, String placeId, String visitId) throws IOException {
        validatePhoto(file);   // Validates size, type, and extension

        // GridFS metadata: associates the photo with a specific place and visit
        Document metadata = new Document()
            .append("placeId", placeId)
            .append("visitId", visitId)
            .append("uploadDate", LocalDateTime.now().toString());

        // Store in GridFS: MongoDB splits the file into 255KB chunks automatically
        ObjectId fileId = gridFsTemplate.store(
            file.getInputStream(),         // The binary data stream
            file.getOriginalFilename(),    // Original filename (e.g., "photo.jpg")
            file.getContentType(),         // MIME type (e.g., "image/jpeg")
            metadata                       // Custom metadata document
        );

        return fileId.toString();          // Return the GridFS file ID as a string
    }
```

#### Parallel Upload (lines 86–127)

For 3+ files, `parallelStream()` is used. If any upload fails, all already-uploaded photos are cleaned up:

```java
    @Override
    public List<String> savePhotosForVisit(List<MultipartFile> files, String placeId, String visitId) throws IOException {
        if (files.size() <= 2) {
            return savePhotosSequentially(files, placeId, visitId);  // Not worth parallelism overhead
        }

        ConcurrentLinkedQueue<String> uploadedIds = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Exception> errors = new ConcurrentLinkedQueue<>();

        files.parallelStream().forEach(file -> {
            try {
                uploadedIds.add(savePhotoForVisit(file, placeId, visitId));
            } catch (Exception e) {
                errors.add(e);
            }
        });

        // Atomic all-or-nothing: if ANY upload fails, clean up ALL successful ones
        if (!errors.isEmpty()) {
            for (String photoId : uploadedIds) {
                try { deletePhoto(photoId); } catch (Exception e) { /* log and continue */ }
            }
            throw new IOException("Failed to upload " + errors.size() + " photo(s)");
        }

        return new ArrayList<>(uploadedIds);
    }
```

#### Thumbnail Generation with Caching (lines 267–330)

```java
    @Override
    public PhotoData getThumbnail(String photoId, int size) {
        // 1. Check for cached thumbnail in GridFS
        Query thumbnailQuery = new Query(
            Criteria.where("metadata.originalId").is(photoId)
                .and("metadata.thumbnailSize").is(size));

        GridFSFile cachedThumb = gridFsTemplate.findOne(thumbnailQuery);
        if (cachedThumb != null) {
            // Cache hit: return the cached thumbnail
            GridFsResource resource = gridFsTemplate.getResource(cachedThumb);
            return new PhotoData(resource.getInputStream(), "image/jpeg", ...);
        }

        // 2. Cache miss: generate thumbnail from original using Thumbnailator
        PhotoData original = getPhoto(photoId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(original.inputStream())
            .size(size, size)              // Square thumbnail (e.g., 80x80)
            .outputFormat("jpg")           // Always output as JPEG
            .outputQuality(0.8)            // 80% quality (good compression)
            .toOutputStream(outputStream);

        byte[] thumbnailBytes = outputStream.toByteArray();

        // 3. Cache the thumbnail in GridFS with back-reference metadata
        Document metadata = new Document()
            .append("originalId", photoId)         // Links to original photo
            .append("thumbnailSize", size)          // 80, 160, etc.
            .append("type", "thumbnail")            // Distinguishes from original photos
            .append("generatedAt", LocalDateTime.now().toString());

        gridFsTemplate.store(new ByteArrayInputStream(thumbnailBytes),
            "thumb_" + photoId + "_" + size + ".jpg", "image/jpeg", metadata);

        return new PhotoData(new ByteArrayInputStream(thumbnailBytes), "image/jpeg", ...);
    }
```

#### Batch Photo Existence Check (lines 228–264)

Prevents N+1 queries when checking if photos still exist in GridFS:

```java
    @Override
    public Set<String> photosExist(Set<String> photoIds) {
        // Convert string IDs to ObjectIds, filtering out invalid formats
        List<ObjectId> objectIds = photoIds.stream()
            .filter(id -> { try { new ObjectId(id); return true; } catch (IllegalArgumentException e) { return false; } })
            .map(ObjectId::new)
            .toList();

        // ONE query to check ALL photos at once (instead of N individual queries)
        Query query = new Query(Criteria.where("_id").in(objectIds));
        return gridFsTemplate.find(query)
            .into(new ArrayList<>())
            .stream()
            .map(file -> file.getObjectId().toString())
            .collect(Collectors.toSet());
    }
```

### 7.4 DefaultGoogleMapsService.java (427 lines)

Extracts place information from Google Maps URLs using the Places API (New).

#### URL Resolution Flow (lines 66–108)

```java
    @Override
    public PlaceExtractResponse extractPlaceFromUrl(String mapsUrl) {
        // 1. Resolve shortened URLs (goo.gl, maps.app.goo.gl)
        String resolvedUrl = resolveShortUrl(mapsUrl);

        // 2. Try to extract place_id from URL (not always present)
        String placeId = extractPlaceId(resolvedUrl);

        Map<String, Object> placeDetails;
        if (placeId != null) {
            // 3a. Have place_id → direct lookup via Place Details API
            placeDetails = fetchPlaceDetails(placeId);
        } else {
            // 3b. No place_id → extract name from URL, use Text Search API
            String placeName = extractPlaceName(resolvedUrl);
            placeDetails = searchPlaceByText(placeName, resolvedUrl);
        }

        // 4. Map Google's response to our PlaceExtractResponse
        return mapToPlaceExtractResponse(placeDetails);
    }
```

#### Text Search with Location Bias (lines 157–200)

```java
    private Map<String, Object> searchPlaceByText(String placeName, String url) {
        // If URL contains coordinates (@lat,lng), use them as location bias
        Map<String, Object> requestBody = Map.of("textQuery", placeName);

        Matcher coordsMatcher = COORDINATES_PATTERN.matcher(url);  // @37.7,-119.6
        if (coordsMatcher.find()) {
            double lat = Double.parseDouble(coordsMatcher.group(1));
            double lng = Double.parseDouble(coordsMatcher.group(2));

            // Add 5km radius location bias to improve search accuracy
            requestBody = Map.of(
                "textQuery", placeName,
                "locationBias", Map.of(
                    "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", 5000.0)));
        }

        // Call Google Places API (New) with field mask to minimize quota usage
        Map<String, Object> response = restClient.post()
            .uri("/places:searchText")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask",
                "places.id,places.displayName,places.formattedAddress," +
                "places.addressComponents,places.location,places.websiteUri," +
                "places.internationalPhoneNumber,places.rating," +
                "places.userRatingCount,places.reviews")
            .body(requestBody)
            .retrieve()
            .body(Map.class);

        // Return first result
        var places = (List<Map<String, Object>>) response.get("places");
        return places.get(0);
    }
```

### 7.5 DefaultSettingsService.java (56 lines)

Simple but important: manages the singleton settings document and triggers distance invalidation.

```java
@Service
@Transactional
public class DefaultSettingsService implements SettingsService {
    private final SettingsRepository settingsRepository;
    private final DistanceService distanceService;

    // @Lazy prevents circular dependency: SettingsService ↔ DistanceService
    public DefaultSettingsService(SettingsRepository settingsRepository,
                                   @Lazy DistanceService distanceService) { ... }

    @Override
    public Settings getSettings() {
        return settingsRepository.findById("default")
            .orElseGet(() -> settingsRepository.save(Settings.createDefault()));
        // If no settings exist yet, create default (no home location)
    }

    @Override
    public Settings updateSettings(Double latitude, Double longitude) {
        Settings current = getSettings();
        Settings updated = current.withHomeLocation(latitude, longitude);
        Settings saved = settingsRepository.save(updated);

        // CRITICAL: When home location changes, ALL cached distances become invalid
        distanceService.invalidateAllDistances();

        return saved;
    }
}
```

---

## 8. Web Layer (Controllers) — Detailed Code Walkthrough

### 8.1 PlaceController.java (1110 lines) — The Main Controller

This controller handles all place-related operations at `/places`.

#### @ModelAttribute Methods (lines 88–107)

These run before every handler method, making reference data available to all templates:

```java
    @ModelAttribute("countries")
    public List<Country> countries() {
        return referenceDataService.getAllCountries();  // Always available in templates
    }

    @ModelAttribute("statesProvincesByCountry")
    public Map<String, List<StateProvince>> statesProvincesByCountry() {
        return referenceDataService.getAllStatesProvinces().stream()
            .collect(Collectors.groupingBy(StateProvince::countryCode));
        // Groups states by country: {"US": [AL, AK, ...], "CA": [AB, BC, ...], "MX": [...]}
    }

    @ModelAttribute("maxPhotoSizeMB")
    public int maxPhotoSizeMB() {
        return maxPhotoSizeMB;  // From application.properties, displayed in templates
    }
```

#### `listPlaces()` — Server-Side Pagination (lines 111–136)

```java
    @GetMapping
    public String listPlaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastVisit") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {

        int pageSize = Math.min(Math.max(size, 5), 50);  // Clamp between 5 and 50

        List<Place> allPlaces = placeService.findAllVisited();  // Excludes wishlist
        Map<String, DistanceResult> distances = distanceService.getDistances(allPlaces);
        List<Place> sortedPlaces = sortPlaces(allPlaces, distances, sort, direction);
        PlacePage placePage = PlacePage.of(sortedPlaces, page, pageSize, sort, direction);

        model.addAttribute("places", placePage.content());   // Current page's places
        model.addAttribute("placePage", placePage);           // Pagination metadata
        model.addAttribute("distances", distances);           // Distance map for display
        return "places/list";                                 // Full page render
    }
```

#### `sortPlaces()` — Multi-Field Sorting (lines 141–181)

```java
    private List<Place> sortPlaces(List<Place> places, Map<String, DistanceResult> distances,
                                   String sortField, String sortDirection) {
        Comparator<Place> comparator = switch (sortField.toLowerCase()) {
            case "name" -> Comparator.comparing(
                (Place p) -> p.name(), String.CASE_INSENSITIVE_ORDER);

            case "distance" -> Comparator.comparing(
                (Place p) -> {
                    DistanceResult result = distances.get(p.id());
                    if (result != null && result.hasDistance()) return result.miles();
                    return Double.MAX_VALUE;  // No distance → sort to end
                },
                Comparator.nullsLast(Comparator.naturalOrder()));

            case "lastvisit", "lastVisit" -> Comparator.comparing(
                (Place p) -> p.getMostRecentVisit().map(v -> v.date()).orElse(null),
                Comparator.nullsLast(Comparator.naturalOrder()));

            default -> /* same as lastVisit */;
        };

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return places.stream().sorted(comparator).toList();
    }
```

#### `searchPlaces()` — htmx Fragment Response (lines 455–488)

```java
    @GetMapping("/search")
    public String searchPlaces(
            @RequestParam(required = false) String q, ...) {
        List<Place> places = (q == null || q.isBlank())
            ? placeService.findAllVisited()
            : placeService.searchVisited(q);

        // ... sort and paginate ...

        model.addAttribute("searchQuery", q);
        return "places/list :: places-table";   // Return ONLY the table fragment, not the full page
        // htmx swaps this fragment into the existing page
    }
```

#### `toggleFavorite()` — Context-Aware htmx Response (lines 492–514)

```java
    @PostMapping("/{id}/toggle-favorite")
    public String toggleFavorite(@PathVariable String id,
                                 @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                                 @RequestParam(value = "context", required = false) String context,
                                 Model model) {
        Place place = placeService.toggleFavorite(id);

        // If unfavoriting FROM the favorites list, re-render the entire table
        // (because the item should disappear from the list)
        if ("true".equals(hxRequest) && "favorites".equals(context)) {
            List<Place> allPlaces = placeService.findFavorites();
            // ... rebuild pagination ...
            return "places/list :: places-table";  // Entire table re-rendered
        }

        // Default: return just the button + OOB badge update
        model.addAttribute("place", place);
        if ("true".equals(hxRequest)) {
            return "places/fragments/favorite-button :: favorite-toggle";
        }

        return "redirect:/places/" + id;  // Non-htmx fallback
    }
```

#### `convertToVisited()` — Deferred Status Change (lines 658–666)

```java
    @PostMapping("/{id}/convert-to-visited")
    public String convertToVisited(@PathVariable String id, RedirectAttributes redirectAttributes) {
        if (placeService.findById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Place not found");
            return "redirect:/places/wishlist";
        }
        // Don't convert immediately! Redirect to edit page with a flag.
        // The user can add visit details, and status changes only on save.
        redirectAttributes.addFlashAttribute("success", "Add visit details and save to mark as visited.");
        return "redirect:/places/" + id + "/edit?convertToVisited=true";
    }
```

In `updatePlace()`, the `convertToVisited` flag determines the status:

```java
    String status = convertToVisited ? "VISITED" : place.status();
```

#### Photo Serving with ETag Caching (lines 789–820)

```java
    @GetMapping("/photos/{photoId}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String photoId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        String etag = "\"" + photoId + "\"";   // ETag = photo ID (content never changes)

        // Browser already has this photo? Return 304 Not Modified (zero bytes transferred)
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .build();
        }

        // Stream photo from GridFS directly to client (no buffering in memory)
        PhotoService.PhotoData photoData = photoService.getPhoto(photoId);
        InputStreamResource resource = new InputStreamResource(photoData.inputStream());

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
            .eTag(etag)
            .contentType(MediaType.parseMediaType(photoData.contentType()))
            .contentLength(photoData.length())
            .body(resource);
    }
```

**Why 365-day cache?** Photos are immutable — the same ID always returns the same bytes. Once the browser downloads a photo, it never needs to re-download it.

#### `cleanupMissingPhotos()` — Preventing Broken Images (lines 927–982)

```java
    private Place cleanupMissingPhotos(Place place) {
        // 1. Collect ALL photo IDs from ALL visits into one set
        Set<String> allPhotoIds = place.visits().stream()
            .filter(v -> v.photoIds() != null)
            .flatMap(v -> v.photoIds().stream())
            .collect(Collectors.toSet());

        if (allPhotoIds.isEmpty()) return place;

        // 2. Single batch query: which of these IDs still exist in GridFS?
        Set<String> existingPhotoIds = photoService.photosExist(allPhotoIds);

        // 3. Filter each visit's photos to only include existing ones
        List<Visit> cleanedVisits = place.visits().stream()
            .map(visit -> {
                List<String> validPhotoIds = visit.photoIds().stream()
                    .filter(existingPhotoIds::contains)
                    .collect(Collectors.toList());
                return visit.withPhotos(validPhotoIds);
            }).collect(Collectors.toList());

        return new Place(place.id(), place.name(), ..., cleanedVisits, ...);
    }
```

#### `parseGoogleReviews()` — Handling Multiple Date Formats (lines 987–1033)

```java
    private List<Review> parseGoogleReviews(String googleReviewsJson) {
        List<Map<String, Object>> reviewMaps = objectMapper.readValue(googleReviewsJson, ...);

        for (Map<String, Object> reviewMap : reviewMaps) {
            // Handle two possible date formats from Jackson deserialization:
            Object publishedAtObj = reviewMap.get("publishedAt");
            if (publishedAtObj instanceof String) {
                publishedAt = LocalDateTime.parse((String) publishedAtObj);
            } else if (publishedAtObj instanceof List) {
                // Jackson may deserialize as array: [2025, 1, 15, 10, 30, 0, 0]
                List<Integer> dateArray = (List<Integer>) publishedAtObj;
                publishedAt = LocalDateTime.of(
                    dateArray.get(0), dateArray.get(1), dateArray.get(2),
                    dateArray.get(3), dateArray.get(4),
                    dateArray.size() > 5 ? dateArray.get(5) : 0);
            }
        }
    }
```

### 8.2 HomeController.java (41 lines)

Simple dashboard aggregating statistics:

```java
    @GetMapping("/")
    public String home(Model model) {
        Settings settings = settingsService.getSettings();
        var recentPlaces = placeService.findAllVisitedByMostRecentVisit()
            .stream().limit(6).toList();                          // Top 6 most recently visited
        long totalVisits = placeService.findAllVisited().stream()
            .mapToLong(p -> p.visits() != null ? p.visits().size() : 0)
            .sum();                                               // Sum of all visit counts

        model.addAttribute("totalPlaces", placeService.findAllVisited().size());
        model.addAttribute("totalVisits", totalVisits);
        model.addAttribute("recentPlaces", recentPlaces);
        model.addAttribute("wishlistCount", placeService.countWishlist());
        model.addAttribute("favoritesCount", placeService.countFavorites());
        model.addAttribute("homeLocationSet", settings.hasHomeLocation());
        return "index";
    }
```

### 8.3 SettingsController.java (88 lines)

Manages home location with htmx support for partial page updates:

```java
    @PostMapping
    public String updateSettings(@Valid @ModelAttribute("settings") Settings settings,
                                BindingResult bindingResult,
                                @RequestHeader(value = "HX-Request", required = false) String hxRequest, ...) {
        if (bindingResult.hasErrors()) {
            if ("true".equals(hxRequest)) {
                return "settings/fragments :: form-section";  // Return just the form with errors
            }
            return "settings/index";                          // Full page with errors
        }

        Settings updatedSettings = settingsService.updateSettings(
            settings.homeLatitude(), settings.homeLongitude());
        // This triggers invalidateAllDistances() in the service layer

        if ("true".equals(hxRequest)) {
            model.addAttribute("success", "Home location updated successfully");
            return "settings/fragments :: form-section";      // Just the form fragment
        }
        return "redirect:/settings";                          // Full page redirect
    }
```

---

## 9. DTO Layer — Detailed Code Walkthrough

### 9.1 PlacePage.java (67 lines) — In-Memory Pagination

```java
public record PlacePage(
    List<Place> content,       // Items on the current page
    int page,                  // Zero-based page number
    int size,                  // Items per page
    long totalElements,        // Total items across all pages
    int totalPages,            // Total number of pages
    String sortField,          // Current sort field ("name", "distance", "lastVisit")
    String sortDirection,      // "asc" or "desc"
    boolean hasNext,           // True if there's a next page
    boolean hasPrevious        // True if there's a previous page
) {
    public static PlacePage of(List<Place> allPlaces, int page, int size,
                               String sortField, String sortDirection) {
        int totalElements = allPlaces.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Clamp page to valid range [0, totalPages-1]
        int safePage = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));

        // Slice the list for this page
        int fromIndex = safePage * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Place> content = fromIndex < totalElements
            ? allPlaces.subList(fromIndex, toIndex)
            : List.of();

        return new PlacePage(content, safePage, size, totalElements, totalPages,
                            sortField, sortDirection,
                            safePage < totalPages - 1,   // hasNext
                            safePage > 0);               // hasPrevious
    }
}
```

### 9.2 DistanceMatrixResponse.java (92 lines) — Google API Response

Nested records that mirror the JSON structure from Google's Distance Matrix API:

```java
public record DistanceMatrixResponse(
    String status,                              // "OK" or error code
    List<String> destination_addresses,
    List<String> origin_addresses,
    List<Row> rows                              // One row per origin
) {
    public record Row(List<Element> elements) {}  // One element per destination

    public record Element(String status, Distance distance, Duration duration) {
        public boolean isOk() { return "OK".equals(status); }
    }

    public record Distance(int value, String text) {   // value = meters
        public double toMiles() { return value / 1609.344; }
    }

    public record Duration(int value, String text) {   // value = seconds
        public double toMinutes() { return value / 60.0; }
    }

    // Helper for single-origin, multi-destination responses
    public Element getElement(int destinationIndex) {
        if (rows == null || rows.isEmpty()) return null;
        Row row = rows.get(0);                         // Always one origin
        if (row.elements() == null || destinationIndex >= row.elements().size()) return null;
        return row.elements().get(destinationIndex);
    }
}
```

### 9.3 DistanceResult.java (75 lines) — Result with Source Tracking

```java
public record DistanceResult(Double miles, Double durationMinutes, Source source) {

    public enum Source {
        CACHED,       // From Place entity cache (no API call needed)
        API,          // Fresh from Google Distance Matrix API
        FALLBACK,     // Haversine formula (straight-line, no duration)
        UNAVAILABLE   // No home location or no coordinates
    }

    // Factory methods enforce invariants:
    public static DistanceResult cached(Double miles, Double durationMinutes) {
        return new DistanceResult(miles, durationMinutes, Source.CACHED);
    }
    public static DistanceResult fromApi(Double miles, Double durationMinutes) {
        return new DistanceResult(miles, durationMinutes, Source.API);
    }
    public static DistanceResult fallback(Double miles) {
        return new DistanceResult(miles, null, Source.FALLBACK);  // No duration from Haversine
    }
    public static DistanceResult unavailable() {
        return new DistanceResult(null, null, Source.UNAVAILABLE);
    }

    public boolean hasDistance() { return miles != null; }
    public boolean isFallback() { return source == Source.FALLBACK; }
}
```

---

## 10. Utility Classes — Detailed Code Walkthrough

### 10.1 DistanceCalculator.java (113 lines) — Haversine Formula

```java
public final class DistanceCalculator {
    private static final double EARTH_RADIUS_MILES = 3958.8;  // Mean Earth radius

    // Haversine formula: great-circle distance between two points on a sphere
    public static Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;

        double lat1Rad = Math.toRadians(lat1);     // Convert degrees → radians
        double lat2Rad = Math.toRadians(lat2);
        double dLat = lat2Rad - lat1Rad;           // Difference in latitude
        double dLon = Math.toRadians(lon2) - Math.toRadians(lon1);  // Difference in longitude

        // Haversine formula:
        // a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        // c = 2 × atan2(√a, √(1-a))   — central angle in radians
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_MILES * c;              // Distance = radius × angle
    }

    // Display formatting:
    // API result: "125 mi (2h 15min)"
    // Fallback:   "125 mi (approx)"
    // No coords:  "N/A"
    public static String formatDistanceWithDuration(Double miles, Double durationMinutes, boolean isFallback) {
        if (miles == null) return "N/A";
        String distance = DISTANCE_FORMAT.format(Math.round(miles)) + " mi";
        if (isFallback) return distance + " (approx)";         // Haversine = approximate
        if (durationMinutes != null) return distance + " (" + formatDuration(durationMinutes) + ")";
        return distance;
    }
}
```

### 10.2 DurationUtils.java (121 lines) — Human-Friendly Duration Parsing

```java
public class DurationUtils {
    // Regex: optional days + optional hours + optional minutes
    // Matches: "10min", "1h25min", "2d5h30min", "3h", "2d"
    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)min)?$");

    private static final Duration MAX_DURATION = Duration.ofDays(7);

    public static Duration parse(String input) {
        String normalized = input.trim().toLowerCase();
        Matcher matcher = DURATION_PATTERN.matcher(normalized);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid format. Use: 10min, 1h25min, or 2d5h30min");
        }

        // Extract captured groups (null if not present)
        String daysStr = matcher.group(1);      // Group 1: days
        String hoursStr = matcher.group(2);     // Group 2: hours
        String minutesStr = matcher.group(3);   // Group 3: minutes

        if (daysStr == null && hoursStr == null && minutesStr == null) {
            throw new IllegalArgumentException("Must specify at least days, hours, or minutes");
        }

        long days = daysStr != null ? Long.parseLong(daysStr) : 0;
        long hours = hoursStr != null ? Long.parseLong(hoursStr) : 0;
        long minutes = minutesStr != null ? Long.parseLong(minutesStr) : 0;

        Duration duration = Duration.ofDays(days).plusHours(hours).plusMinutes(minutes);

        if (duration.isZero()) throw new IllegalArgumentException("Must be greater than zero");
        if (duration.compareTo(MAX_DURATION) > 0) throw new IllegalArgumentException("Cannot exceed 7 days");

        return duration;
    }

    // Format: Duration → "1 hour 25 minutes", "2 days 5 hours"
    public static String format(Duration duration) {
        if (duration == null || duration.isZero()) return "";
        long totalMinutes = duration.toMinutes();
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day" : " days");
        if (hours > 0) { if (sb.length() > 0) sb.append(" "); sb.append(hours).append(hours == 1 ? " hour" : " hours"); }
        if (minutes > 0) { if (sb.length() > 0) sb.append(" "); sb.append(minutes).append(minutes == 1 ? " minute" : " minutes"); }
        return sb.toString();
    }
}
```

---

## 11. Configuration and Startup — Detailed Code Walkthrough

### 11.1 MongoConfig.java (18 lines)

```java
@Configuration
@EnableMongoRepositories(basePackages = "dk.placestracker.domain.repository")
// ↑ Tells Spring Data where to scan for MongoRepository interfaces
@EnableMongoAuditing
// ↑ Enables @CreatedDate/@LastModifiedDate annotations (not currently used — timestamps are manual)
public class MongoConfig {
    // All configuration via application.properties
    // Auto-index creation enabled: spring.data.mongodb.auto-index-creation=true
}
```

### 11.2 ReferenceDataLoader.java (173 lines)

Implements `ApplicationRunner` — Spring Boot calls `run()` after the application context is fully initialized.

```java
@Component
public class ReferenceDataLoader implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        loadCountries();          // Load 3 North American countries
        loadStatesProvinces();    // Load 95 states/provinces/territories
    }

    private void loadCountries() {
        if (countryRepository.count() > 0) {
            return;   // Already loaded — idempotent (safe to run multiple times)
        }
        List<Country> countries = List.of(
            Country.create("US", "United States", 1),   // displayOrder determines dropdown order
            Country.create("CA", "Canada", 2),
            Country.create("MX", "Mexico", 3)
        );
        countryRepository.saveAll(countries);
    }

    private void loadStatesProvinces() {
        if (stateProvinceRepository.count() > 0) return;

        List<StateProvince> statesProvinces = List.of(
            // 50 US states (AL through WY)
            StateProvince.create("AL", "Alabama", "US", 1),
            StateProvince.create("AK", "Alaska", "US", 2),
            // ... (48 more US states)

            // 13 Canadian provinces/territories
            StateProvince.create("AB", "Alberta", "CA", 1),
            StateProvince.create("BC", "British Columbia", "CA", 2),
            // ... (11 more)

            // 32 Mexican states
            StateProvince.create("AGS", "Aguascalientes", "MX", 1),
            StateProvince.create("BC", "Baja California", "MX", 2),
            // ... (30 more)
        );
        stateProvinceRepository.saveAll(statesProvinces);
    }
}
```

---

## 12. Frontend Architecture — Detailed Code Walkthrough

### 12.1 Layout System

The application uses the **Thymeleaf Layout Dialect** for template inheritance. `layout.html` defines the master template with navigation, flash messages, footer, and all CSS/JS dependencies. Child templates declare `layout:decorate="~{layout}"` and fill `layout:fragment="content"`.

### 12.2 CSS Theme (custom.css — 1592 lines)

Key design tokens:

```css
:root {
    --primary-500: #06B6D4;   /* Vibrant Teal — primary brand color */
    --accent-500: #F97316;     /* Coral/Orange — hover/interaction color */
    --success-500: #10B981;    /* Fresh Green */
    --font-display: 'Nunito', sans-serif;
    --font-body: 'Plus Jakarta Sans', sans-serif;
    --radius-lg: 12px;
}
```

Buttons use a vibrant hover transition (teal → coral):

```css
.btn-success { background: var(--primary-600); }
.btn-success:hover {
    background: var(--accent-500);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(249, 115, 22, 0.35);
}
```

Action buttons in list views use a flex layout with proper spacing:

```css
.action-btns {
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
}
.action-btns .btn { border-radius: var(--radius-md); }
.action-btns form { display: inline-flex; }
```

### 12.3 htmx Integration Patterns

**Live Search** — Debounced input triggers server-side search:

```html
<input type="search"
       th:hx-get="@{/places/search}"          <!-- Server endpoint -->
       hx-trigger="keyup changed delay:300ms"   <!-- Wait 300ms after typing stops -->
       hx-target="#places-table"                 <!-- Replace this element -->
       hx-swap="outerHTML"                       <!-- Replace the entire element -->
       hx-include="#pagination-state"            <!-- Include hidden sort/size params -->
       hx-indicator="#search-indicator">          <!-- Show spinner during request -->
```

**Favorite Toggle with OOB Swap** — The button replaces itself AND updates a badge elsewhere:

```html
<!-- Main response (replaces the button) -->
<button th:hx-post="@{/places/{id}/toggle-favorite(id=${place.id})}"
        hx-target="this" hx-swap="outerHTML">
    <i class="bi" th:classappend="${place.favorite()} ? 'bi-star-fill' : 'bi-star'"></i>
</button>

<!-- OOB swap (updates the badge in the header, if it exists on the page) -->
<span id="favorite-badge" hx-swap-oob="true">
    <span th:if="${place.favorite()}" class="badge bg-warning text-dark">
        <i class="bi bi-star-fill me-1"></i>Favorite
    </span>
</span>
```

**Context-Aware Responses** — When unfavoriting from the favorites list:

```html
<button th:hx-post="@{/places/{id}/toggle-favorite(id=${place.id})}"
        hx-target="#places-table" hx-swap="outerHTML"
        hx-vals='{"context": "favorites"}'>   <!-- Tells server to re-render full table -->
```

### 12.4 JavaScript Modules

**confirm-modal.js (290 lines)** — Custom confirmation modal replacing `confirm()`. Intercepts form submissions with `data-confirm` attributes, htmx `hx-confirm` events, and button clicks. Centralized message configurations:

```javascript
const ConfirmModal = {
    messages: {
        'delete-place': { title: 'Delete Place', message: 'All visits and photos will be removed.', type: 'danger' },
        'convert-to-visited': { title: 'Mark as Visited', message: 'Move from wishlist to visited?', type: 'info' },
        // ... more message keys
    },
    // Intercepts form submissions:
    // document.addEventListener('submit', function(e) {
    //     if (form.getAttribute('data-confirm')) { e.preventDefault(); self.show(confirmKey); }
    // });
};
```

**google-maps-autofill.js (124 lines)** — Handles extracting place data from Google Maps URLs via the API endpoint, then populates form fields.

**visit-management.js (251 lines)** — Manages dynamic visit addition/removal on place forms with UUID generation and field reindexing.

**visit-photo-upload.js (72 lines)** — Handles bulk photo uploads per visit with validation and progress display.

**settings-autofill.js (32 lines)** — Overrides the `populateFormFields` function for the settings page to map coordinates to `homeLatitude`/`homeLongitude` fields.

### 12.5 Thymeleaf Template Pitfall

`th:if`/`th:unless` + `th:replace` on the same element is unreliable:

```html
<!-- WRONG: th:replace can override th:unless -->
<div th:unless="${place.isWishlist()}"
     th:replace="~{places/fragments/favorite-button :: favorite-btn}"></div>

<!-- CORRECT: Separate conditional from replacement -->
<th:block th:unless="${place.isWishlist()}">
    <div th:replace="~{places/fragments/favorite-button :: favorite-btn}"></div>
</th:block>
```

---

## 13. Testing Strategy — Detailed Code Walkthrough

### 13.1 Test Infrastructure

Tests use **Testcontainers** for MongoDB with `@ServiceConnection`:

```java
public abstract class AbstractMongoTest {
    @Container
    @ServiceConnection       // Spring Boot auto-configures the MongoDB connection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");
}
```

### 13.2 Test Classes

| Test Class | Type | Lines | Description |
|-----------|------|-------|-------------|
| `PlaceRepositoryTests` | Integration | ~455 | MongoDB queries with real database |
| `DefaultPlaceServiceTests` | Unit | ~300 | Service logic with mocked repository |
| `DefaultDistanceServiceTests` | Unit | ~200 | Distance calculation and caching |
| `DefaultSettingsServiceTests` | Unit | ~100 | Settings CRUD and cache invalidation |
| `PlaceControllerTests` | WebMvc | ~600 | Controller endpoints with MockMvc |
| `HomeControllerTests` | WebMvc | ~100 | Dashboard rendering |
| `SettingsControllerTests` | WebMvc | ~150 | Settings endpoints |
| `VisitTests` | Unit | 275 | Visit record methods |
| `DistanceCalculatorTests` | Unit | ~100 | Haversine formula |
| `DurationUtilsTests` | Unit | ~100 | Duration parsing/formatting |

All test classes use the `*Tests` suffix (plural) per Spring convention.

---

## 14. Deployment — Detailed Code Walkthrough

### 14.1 Dockerfile — Multi-Stage Build

```dockerfile
# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY gradlew gradle build.gradle settings.gradle .    # Copy build files first (layer caching)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || return 0     # Download dependencies (cached layer)
COPY src src                                           # Copy source code (changes more often)
RUN ./gradlew bootJar --no-daemon -x test              # Build JAR, skip tests

# Stage 2: Runtime image (JDK for keytool SSL cert generation)
FROM eclipse-temurin:25-jdk
WORKDIR /app
RUN groupadd -r placestracker && useradd -r -g placestracker placestracker   # Non-root user
COPY --from=builder /app/build/libs/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN mkdir -p /app/logs /app/certs && chown -R placestracker:placestracker /app
USER placestracker                                     # Run as non-root
EXPOSE 8080 8443
ENTRYPOINT ["/app/docker-entrypoint.sh"]
```

### 14.2 docker-entrypoint.sh — SSL Certificate Generation

```bash
#!/bin/bash
set -e

KEYSTORE_PATH="${CERTS_DIR}/placestracker-keystore.p12"

# Generate SSL certificate only if it doesn't already exist
if [ ! -f "${KEYSTORE_PATH}" ]; then
    # Build SAN (Subject Alternative Name) from comma-separated hosts and IPs
    # SAN_PARTS = "DNS:localhost,DNS:placestracker.local,IP:127.0.0.1,IP:0.0.0.0"

    keytool -genkeypair \
        -alias "${CERT_ALIAS}" \
        -keyalg RSA -keysize 2048 \
        -storetype PKCS12 \
        -keystore "${KEYSTORE_PATH}" \
        -storepass "${CERT_PASSWORD}" \
        -validity "${CERT_VALIDITY}" \           # Default: 3650 days (10 years)
        -dname "CN=${CERT_CN}, OU=Places Tracker Docker, ..." \
        -ext "SAN=${SAN_PARTS}"
fi

# Start the application
exec java ${JAVA_OPTS:--Xms512m -Xmx1g} -jar app.jar --spring.profiles.active=docker "$@"
```

### 14.3 docker-compose.standalone.yml — Full Stack

```yaml
services:
  mongodb:
    image: mongo:8.0
    restart: unless-stopped
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh localhost:27017/placestracker --quiet
      interval: 10s
      retries: 5
      start_period: 40s

  app:
    build: .
    ports: ["8443:8443"]
    environment:
      SPRING_MONGODB_URI: mongodb://mongodb:27017/placestracker    # Internal Docker networking
      SERVER_SSL_KEY_STORE: file:/app/certs/placestracker-keystore.p12
      GOOGLE_MAPS_API_KEY: ${GOOGLE_MAPS_API_KEY}
      JAVA_OPTS: "-Xms512m -Xmx1g"
    depends_on:
      mongodb: { condition: service_healthy }    # Wait for MongoDB to be healthy
    volumes:
      - app_certs:/app/certs     # Persistent SSL certificates
      - app_logs:/app/logs       # Persistent application logs
```

### 14.4 Environment Variables

```bash
GOOGLE_MAPS_API_KEY=...          # Required: enables auto-fill and driving distances
SPRING_MONGODB_URI=...           # MongoDB connection string
SERVER_PORT=8443                 # Server port
CERT_PASSWORD=...                # SSL keystore password
CERT_HOSTS=localhost,...         # Hostnames for SSL certificate SAN
CERT_IPS=127.0.0.1,...          # IP addresses for SSL certificate SAN
JAVA_OPTS="-Xms512m -Xmx1g"    # JVM memory settings
```

---

*Generated from the Places Tracker codebase, version 1.2.0-SNAPSHOT*
