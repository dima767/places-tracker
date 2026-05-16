# Places Tracker

A personal web app for tracking visited places (parks, trails, restaurants, landmarks, scenic byways - whatever you like to explore). Built with Spring Boot 4 on Java 25 and a single MongoDB instance. Server-rendered HTML with htmx for interactivity, no SPA framework.

> This is a personal tool. There's no hosted public instance to try - the repo is here for code reading and so anyone who wants the same thing for themselves can run their own copy.

## What it does

- **Places and visits as separate concepts** - a place is a fixed spot in the world; visits are moments in time. Each place has a `List<Visit>` with date, temperature, notes, duration, and photos.
- **Google Maps quick-fill** - paste a Google Maps share link on the add-place form, click *Fill from URL*, and the place name, address, coordinates, country/state, Google rating, reviews, and Place ID auto-populate via the Places API (New).
- **Driving distance from home** - cached per-place with a home-location fingerprint, so cache invalidates automatically when you move. Uses Google Distance Matrix with Haversine fallback when the API key isn't configured.
- **Photo gallery per visit** - upload JPEG/PNG/WebP, stored in MongoDB GridFS, with on-demand thumbnails (also cached in GridFS). Parallel upload with rollback on partial failure.
- **Favorites and wishlist** - mark places as favorite, or save not-yet-visited places to a wishlist; "visit" a wishlist item to flip it to visited and record the first visit in one step.
- **Search, sort, filter** - on the places list and the wishlist.
- **Dark mode** - automatic, based on system preference.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0 |
| Database | MongoDB 8.0 (with GridFS for photo storage) |
| Web | Thymeleaf + htmx + Bootstrap 5.3 |
| Build | Gradle 9.2 (wrapper included) |
| Tests | JUnit 6, Testcontainers (real Mongo in tests) |
| Deploy | Docker Compose; ships to exe.dev VM via included script |

## Quick start

### Option A: Everything in Docker (recommended)

```bash
git clone https://github.com/dima767/places-tracker.git
cd places-tracker
cp .env.example .env
# Edit .env and set GOOGLE_MAPS_API_KEY=...
./compose-up-standalone.sh
```

App at **https://localhost:8143/placestracker/** (accept the self-signed cert warning).

Stop with `./compose-down-standalone.sh`.

### Option B: Mongo in Docker, app on the host

For when you want hot-reload via `gradle bootRun`:

```bash
cp .env.example .env
./compose-up.sh                # starts Mongo only
export GOOGLE_MAPS_API_KEY=...
./gradlew bootRun
```

App at **https://localhost:8143/placestracker/**. Stop Mongo with `./compose-down.sh`.

### Option C: Fully native

If you'd rather run Mongo yourself (Homebrew, Linux package, etc.):

```bash
brew install mongodb-community@8.0
brew services start mongodb-community@8.0
export GOOGLE_MAPS_API_KEY=...
./gradlew bootRun
```

## Configuration

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GOOGLE_MAPS_API_KEY` | Yes (for Maps features) | - | Google Cloud API key |
| `SPRING_MONGODB_URI` | No | `mongodb://localhost:27017/placestracker` | Mongo connection string |
| `CERT_PASSWORD` | No | `placestracker-dev-cert` | Password for the auto-generated HTTPS cert (Docker only) |
| `CERT_HOSTS` | No | `localhost,placestracker.local` | Hostnames in the self-signed cert |
| `JAVA_OPTS` | No | `-Xms512m -Xmx1g` | JVM memory settings |

### Google Cloud setup

1. Create or pick a project in the [Google Cloud Console](https://console.cloud.google.com/).
2. Enable: **Places API (New)**, **Maps JavaScript API**, **Distance Matrix API**.
3. Create an API key under **APIs & Services → Credentials**.
4. Restrict the key (HTTP referrer or IP allowlist) and set a billing quota cap before going to production.
5. Put the key in `.env` (local) or `.env.prod` (VM deploy).

## SSL / HTTPS

The app runs HTTPS in all modes. There are two distinct keystores:

- **Dev keystore** (`src/main/resources/ssl/keystore.p12`) - committed, self-signed, password is the literal `changeit`. Loaded automatically when you run `./gradlew bootRun` on the host. Useless to attackers (default password, self-signed, only works for `localhost`). Browser will warn; accept and move on.
- **Container keystore** - auto-generated at container start by `docker-entrypoint.sh` from the `CERT_PASSWORD` / `CERT_HOSTS` / `CERT_IPS` env vars. Used by both `docker-compose.standalone.yml` and the production deploy.

## Docker Compose layouts

| File | Purpose | Helper scripts |
|---|---|---|
| `docker-compose.yml` | MongoDB only (use with `gradle bootRun`) | `compose-up.sh`, `compose-down.sh` |
| `docker-compose.standalone.yml` | App + MongoDB, everything containerised | `compose-up-standalone.sh`, `compose-down-standalone.sh` |
| `docker-compose.local.yml` | App in Docker, Mongo over SSH tunnel to a remote VM | `compose-up-local.sh`, `compose-down-local.sh` |
| `docker-compose.prod.yml` | Production deploy on a VM (used by `deploy-to-exe.sh`) | n/a (driven by deploy script) |

## Deploying to exe.dev

There's a one-shot deploy script for [exe.dev](https://exe.dev) VMs:

```bash
cp .deploy-config.example .deploy-config         # fill in VM_HOST, VM_USER, etc.
cp .env.prod.example .env.prod                   # production env vars
./deploy-to-exe.sh
```

The script SSHs to the VM, clones/pulls the repo, copies `.env.prod` to the VM as `.env` with 600 perms, builds the Docker image, and starts the containers via `docker-compose.prod.yml`. The exe.dev proxy is configured automatically to point the public hostname at the container.

Helper scripts for managing the running deployment:

| Script | What it does |
|---|---|
| `status-exe.sh` | Show running containers and health |
| `logs-exe.sh` | Tail app + db logs from the VM |
| `logs-app.sh` / `logs-db.sh` / `logs-all.sh` | Local equivalents for the Docker Compose stacks |
| `start-exe.sh` / `stop-exe.sh` | Start/stop the running stack on the VM (without redeploying) |
| `start-db-exe.sh` / `stop-db-exe.sh` | Start/stop just Mongo on the VM |
| `db-tunnel.sh` | Open an SSH tunnel from `localhost:27017` to the VM's Mongo (use with `dev-remote-db.sh`) |
| `dev-remote-db.sh` | Run the app locally against the tunneled VM Mongo |
| `backup-db.sh` | `mongodump` against the configured Mongo and write to `backups/` |
| `restore-db.sh` | `mongorestore` from a backup dump |
| `build-and-run.sh` | Build a fat jar and run it directly (no Docker) |

## Development

```bash
./gradlew test          # runs tests via Testcontainers (needs Docker)
./gradlew bootJar       # produces build/libs/places-tracker-1.1.3.jar
./gradlew build         # full build + tests
```

Hot reload via Spring Boot DevTools is enabled - changes to templates and Java code trigger an automatic restart in `bootRun` mode.

### Code layout

```
src/main/java/dk/placestracker/
├── PlacesTrackerApplication.java
├── config/        # SecurityConfig, RestClientConfig, etc.
├── domain/
│   ├── model/     # Place, Visit, Review, Settings (Java records)
│   └── repository/
├── service/       # PlaceService, PhotoService, GoogleMapsService, DistanceService, ...
├── util/          # DurationUtils, DistanceCalculator
└── web/
    ├── controller/  # HomeController, PlaceController, SettingsController
    └── dto/

src/main/resources/
├── application.properties
├── static/        # CSS, JS, images, lightbox assets
├── templates/     # Thymeleaf views
│   ├── index.html, layout.html
│   ├── places/    # list, detail, create, edit, wishlist views
│   └── settings/
└── ssl/keystore.p12
```

## Security notes

- `.env`, `.env.prod`, `.deploy-config`, `.claude/` are all gitignored. Only `.*.example` template files are committed.
- The dev keystore is committed *deliberately* with a known-default password because it's used only for `localhost` HTTPS in development. Don't reuse it anywhere else.
- The production keystore is generated fresh inside the container at startup, never committed, never reused.
- Run `npm audit` / `./gradlew dependencies` periodically and watch the GitHub security tab. PRs that touch dependencies should explain why.

## License

MIT - see [LICENSE](LICENSE).

## Acknowledgments

Built with [Spring Boot](https://spring.io/projects/spring-boot), [htmx](https://htmx.org/), [Bootstrap](https://getbootstrap.com/), [Thumbnailator](https://github.com/coobird/thumbnailator), and [Lightbox2](https://lokeshdhakar.com/projects/lightbox2/). UI/UX work and several backend pieces (Mongo + GridFS, Google Maps URL parsing) were done in collaboration with Claude Code.
