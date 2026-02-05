# Refactoring Plan: Park Tracker → Places Tracker

## Overview
Rename the entire application from "Park Tracker" to "Places Tracker", changing all references throughout code, UI, URLs, database, and documentation.

## Scope Summary
- **59+ files** requiring modifications
- **Package rename**: `dk.parktracker` → `dk.placestracker`
- **Entity rename**: `Park` → `Place`, `Visit` remains unchanged
- **URL paths**: `/parks` → `/places`, `/parktracker` → `/placestracker`
- **MongoDB collection**: `parks` → `places`
- **Context path**: `/parktracker` → `/placestracker`

---

## Phase 1: Java Domain Model & Services

### 1.1 Rename Core Entities
| Current | New |
|---------|-----|
| `Park.java` | `Place.java` |
| `ParkService.java` | `PlaceService.java` |
| `DefaultParkService.java` | `DefaultPlaceService.java` |
| `ParkRepository.java` | `PlaceRepository.java` |

**Files:**
- `src/main/java/dk/parktracker/domain/Park.java` → `Place.java`
- `src/main/java/dk/parktracker/service/ParkService.java` → `PlaceService.java`
- `src/main/java/dk/parktracker/service/DefaultParkService.java` → `DefaultPlaceService.java`
- `src/main/java/dk/parktracker/repository/ParkRepository.java` → `PlaceRepository.java`

### 1.2 Update MongoDB Collection Annotation
```java
// In Place.java
@Document(collection = "places")  // was "parks"
public record Place(...)
```

### 1.3 Update Photo Service
- `PhotoService.java` - Change method params from `parkId` to `placeId`
- `DefaultPhotoService.java` - Update metadata field names
- GridFS metadata: `parkId` → `placeId`

### 1.4 Update Google Maps Service
- `PlaceExtractResponse.java` - Already uses "place" terminology (keep as-is)
- `GoogleMapsService.java` - No changes needed (generic)

---

## Phase 2: Controllers & Web Layer

### 2.1 Rename Controllers
| Current | New |
|---------|-----|
| `ParkController.java` | `PlaceController.java` |

**URL Mappings to change:**
- `@RequestMapping("/parks")` → `@RequestMapping("/places")`
- All `@GetMapping`/`@PostMapping` paths

### 2.2 Update Controller Methods
- All occurrences of `park` variable → `place`
- Model attributes: `"park"` → `"place"`, `"parks"` → `"places"`
- Form binding: `@ModelAttribute Park` → `@ModelAttribute Place`
- Redirect URLs: `redirect:/parks` → `redirect:/places`

### 2.3 Update Home Controller
- `HomeController.java` - Update any park references
- Dashboard stats: "parks" → "places"

---

## Phase 3: Thymeleaf Templates

### 3.1 Rename Template Directories
```
src/main/resources/templates/parks/ → templates/places/
```

### 3.2 Update Template Files
| Current | New |
|---------|-----|
| `templates/parks/list.html` | `templates/places/list.html` |
| `templates/parks/detail.html` | `templates/places/detail.html` |
| `templates/parks/edit.html` | `templates/places/edit.html` |
| `templates/parks/create.html` | `templates/places/create.html` |
| `templates/parks/fragments/*.html` | `templates/places/fragments/*.html` |

### 3.3 Update Template Content
In ALL templates:
- `th:href="@{/parks}"` → `th:href="@{/places}"`
- `th:action="@{/parks/...}"` → `th:action="@{/places/...}"`
- `${park}` → `${place}`
- `${parks}` → `${places}`
- `th:each="park : ${parks}"` → `th:each="place : ${places}"`
- Text content: "Parks" → "Places", "Park" → "Place"

### 3.4 Update Layout Template
- `layout.html` - Navigation links, title, branding
- "Park Tracker" → "Places Tracker"

### 3.5 Update Fragment Includes
- All `th:replace="~{parks/fragments/...}"` → `th:replace="~{places/fragments/...}"`

---

## Phase 4: Static Resources

### 4.1 JavaScript Files
- `google-maps-autofill.js` - Update endpoint paths
- `visit-management.js` - Update any park references
- `visit-photo-upload.js` - Update endpoint paths
- `confirm-modal.js` - Update message keys (`delete-park` → `delete-place`)

### 4.2 CSS Files
- `custom.css` - Update any park-specific class names if present

---

## Phase 5: Configuration

### 5.1 Application Properties
```properties
# application.properties
server.servlet.context-path=/placestracker  # was /parktracker
```

### 5.2 Update Package Structure
Rename entire package hierarchy:
```
src/main/java/dk/parktracker/ → src/main/java/dk/placestracker/
src/test/java/dk/parktracker/ → src/test/java/dk/placestracker/
```

All Java files need package declaration updated.

---

## Phase 6: Tests

### 6.1 Rename Test Files
| Current | New |
|---------|-----|
| `ParkControllerTests.java` | `PlaceControllerTests.java` |
| `ParkServiceTests.java` | `PlaceServiceTests.java` |
| `ParkRepositoryTests.java` | `PlaceRepositoryTests.java` |
| `ParkControllerIntegrationTests.java` | `PlaceControllerIntegrationTests.java` |

### 6.2 Update Test Content
- All test method names referencing "park"
- All test data using "park" strings
- MockMvc paths: `/parks` → `/places`
- Assertions checking for "park" in responses

---

## Phase 7: Documentation & Project Files

### 7.1 Update Documentation
- `CLAUDE.md` - All references to Park Tracker, parktracker
- `README.md` (if exists)

### 7.2 Update Build Configuration
- `build.gradle` - Group/artifact if named after park

### 7.3 Update Scripts (if any)
- Docker files
- Shell scripts
- CI/CD configurations

---

## Execution Order (Recommended)

1. **Package rename first** - Use IDE refactoring or careful find/replace
2. **Domain model** - Park.java → Place.java with all internal references
3. **Services** - Update service interfaces and implementations
4. **Repository** - Rename and update collection annotation
5. **Controller** - Rename and update all mappings
6. **Templates** - Rename directory and update all files
7. **JavaScript** - Update endpoints and message keys
8. **Configuration** - Context path and any other settings
9. **Tests** - Rename and update all test files
10. **Documentation** - CLAUDE.md and other docs

---

## Database Migration Note

**Existing MongoDB Data:**
- The `parks` collection will need to be renamed to `places`
- Or: Update the `@Document(collection = "parks")` to keep using old collection name for backward compatibility

**Recommended approach:** Rename collection in MongoDB:
```javascript
db.parks.renameCollection("places")
```

---

## Verification Steps

1. **Build**: `./gradlew clean build` - Ensure compilation succeeds
2. **Tests**: `./gradlew test` - All 77+ tests pass
3. **Run**: Start application, verify:
   - Home page loads at `/placestracker/`
   - Places list at `/placestracker/places`
   - Create/Edit/Delete places work
   - Photo upload/view works
   - Search functionality works
4. **UI Check**: Verify all text shows "Place(s)" not "Park(s)"
5. **URLs**: Confirm no `/parks` or `/parktracker` URLs remain

---

## Risk Mitigation

- **Create git branch** before starting: `git checkout -b refactor/places-tracker`
- **Commit frequently** after each phase
- **Run tests** after each major change
- **Keep backup** of working state

---

## Estimated File Changes

| Category | Files |
|----------|-------|
| Java (main) | ~20 |
| Java (test) | ~15 |
| Templates | ~15 |
| JavaScript | ~5 |
| CSS | ~1 |
| Config | ~3 |
| **Total** | **~59** |
