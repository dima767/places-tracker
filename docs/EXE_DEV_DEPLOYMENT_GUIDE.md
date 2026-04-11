# Places Tracker - exe.dev Deployment Guide

Step-by-step guide for deploying Places Tracker to an exe.dev VM from a clean slate. Security-first: private VM, no secrets in git, MongoDB bound to loopback on the VM.

## What you get

- Spring Boot 4 app + MongoDB 8 running on a private exe.dev VM
- HTTPS via exe.dev's proxy (no cert management)
- exe.dev auth gate in front of the app (private share)
- A shared MongoDB that both the VM app and your local app can use via an SSH tunnel - no split-brain between dev and prod data
- A set of scripts that mirror Marcus's deployment flow (deploy, start, stop, logs, db-tunnel, etc.)

## Prerequisites

- exe.dev account with SSH access configured (`ssh exe.dev whoami` works)
- GitHub repo with the Places Tracker source (private or public)
- Google Maps API key (`GOOGLE_MAPS_API_KEY`)
- Docker running locally (for the local-container mode)

---

## Step 1 - Create the VM on exe.dev

```
ssh exe.dev new --name=your-app
```

This creates `your-app.exe.xyz`. exe.dev VMs are **private by default** - the HTTPS proxy requires auth to your exe.dev account. We'll verify in Step 7.

## Step 2 - Wire up GitHub access on the VM

If the repo is private, the VM can't `git clone` without credentials. Use exe.dev's GitHub integration so tokens stay on exe.dev's side:

```
ssh exe.dev integrations add github --name your-integration --repository <user>/places-tracker --attach vm:your-app
```

Follow the prompts to authorize GitHub. After this, the VM can clone over HTTPS with no token management.

The command prints an internal URL like:

```
https://your-integration.int.exe.xyz/<user>/places-tracker.git
```

Save that - you'll paste it into `.deploy-config` in the next step.

If the repo is public, you can skip the integration and use the regular `https://github.com/<user>/places-tracker.git` URL.

## Step 3 - Fill in `.deploy-config` locally

```
cd /Users/dkopylenko/work/Claude/places-tracker
cp .deploy-config.example .deploy-config
```

Edit `.deploy-config`:
- `VM_HOST=your-app.exe.xyz`
- `VM_USER=exedev`
- `APP_DIR=/home/exedev/places-tracker`
- `REPO_URL=https://your-integration.int.exe.xyz/<user>/places-tracker.git` (from Step 2)

## Step 4 - Fill in `.env.prod` locally

```
cp .env.prod.example .env.prod
```

Edit `.env.prod`:
- `MONGO_INITDB_DATABASE=placestracker`
- `GOOGLE_MAPS_API_KEY=<your key>`
- `JAVA_OPTS=-Xms256m -Xmx512m`

Both `.deploy-config` and `.env.prod` are in `.gitignore`. Never commit them.

## Step 5 - Sanity check SSH to VM

```
ssh exedev@your-app.exe.xyz echo ok
```

If this fails, check your SSH keys:
```
ssh exe.dev ssh-key list
```

exe.dev auto-provisions your SSH key, so this should just work.

## Step 6 - Deploy

```
./deploy-to-exe.sh
```

What this does (all over encrypted SSH/SCP):
1. Connects to `exedev@your-app.exe.xyz`
2. Clones (or pulls) the repo on the VM via the exe.dev GitHub integration
3. SCPs `.env.prod` to `${APP_DIR}/.env` with **600** perms
4. Runs `docker compose -f docker-compose.prod.yml up -d --build` on the VM
5. Configures exe.dev proxy: forwards public HTTPS to container port 8080

## Step 7 - Verify private access

```
ssh exe.dev share show your-app
```

Confirm it says **private** (not public). If it's public for any reason:
```
ssh exe.dev share set-private places-tracker
```

Open `https://your-app.exe.xyz/placestracker/` in a browser - you should be prompted to log in with exe.dev. That login gate is the auth layer.

## Step 8 - Smoke test

```
./status-exe.sh
./logs-exe.sh app
```

App should be serving on 8080 (bound to 127.0.0.1, proxied by exe.dev), MongoDB healthy on 27017 (also 127.0.0.1 only).

---

## Security checklist

| Surface | Protection |
|---|---|
| VM HTTPS endpoint | exe.dev auth gate (private share) - only you can reach it |
| TLS termination | exe.dev proxy (auto cert, you don't manage it) |
| Key transfer | SSH/SCP only - never over HTTP, never via git |
| `.env` on VM | 600 perms, contains Google Maps key and DB name |
| Secrets in image | None - `.dockerignore` excludes `.env*`, `.deploy-config` |
| Secrets in git | `.deploy-config`, `.env.prod` in `.gitignore` |
| MongoDB port | Bound to `127.0.0.1` on VM - reachable only via SSH tunnel, not exposed to internet |
| App port 8080 | Bound to `127.0.0.1` on VM, exe.dev proxy is the only public path |
| App-level SSL | Disabled in prod - exe.dev handles TLS, Spring Boot speaks plain HTTP internally |
| Forwarded headers | `SERVER_FORWARD_HEADERS_STRATEGY=native` so Spring sees the real client IP / scheme |

---

## Shared database - one MongoDB for local and VM

Both the local dev app and the VM app use the same MongoDB instance running on the VM. This keeps place/visit/photo data consistent when you switch between running locally and running on the VM - no more "why isn't my test place showing up in prod" moments.

### How it works

- MongoDB runs on the VM as a Docker container, exposed on the VM's `127.0.0.1:27017`
- The VM app connects directly over Docker's internal network (`mongodb://mongodb:27017/...`)
- Your local app connects through an SSH tunnel: local `:27017` -> VM `:27017`
- No port is exposed to the internet - SSH is the only way in

### Local development with VM database

Two options depending on how you want to run the app:

**Option A: Run as JAR from IDE or gradlew**

1. Make sure VM MongoDB is up: `./start-db-exe.sh`
2. Open the tunnel: `./db-tunnel.sh`
3. Run the app from your IDE (it connects to `localhost:27017` transparently because `spring.mongodb.uri` defaults to that)
4. When done: `./db-tunnel.sh stop`

Or one-shot:

```
./dev-remote-db.sh       # checks VM DB, opens tunnel, stops any local mongo that would conflict
```

**Option B: Run in a local container (closer to production)**

```
./compose-up-local.sh -b   # builds image, sets up tunnel, starts app container
```

This runs the app inside Docker but connects to the VM database through the SSH tunnel. Uses `docker-compose.local.yml` which has **no** MongoDB service; the app connects to the host's `localhost:27017` via `host.docker.internal`.

```
./compose-down-local.sh      # stop app container (tunnel stays open)
./compose-down-local.sh -v   # stop and remove volumes
```

### Avoiding port conflicts

If your local `docker-compose.yml` or `docker-compose.standalone.yml` is already running a Mongo container on `:27017`, the tunnel can't bind. `dev-remote-db.sh` and `compose-up-local.sh` both detect this and stop the local Mongo before opening the tunnel. If you open the tunnel manually with `db-tunnel.sh`, make sure no local Mongo container is up first:

```
docker compose -f docker-compose.standalone.yml down
docker compose -f docker-compose.yml down
```

---

## Day-to-day operations

### Redeploy after code changes

```
git push
./deploy-to-exe.sh
```

This pulls the latest code on the VM and runs `docker compose up -d --build`. Docker Compose is smart about what it restarts:

- **Only code changed** - app image is rebuilt and app container recreated. MongoDB is untouched.
- **Nothing changed** - both left alone.
- **App not running** (e.g. after stop) - MongoDB stays up, app gets built and started.
- **MongoDB config changed** in `docker-compose.prod.yml` - MongoDB is also recreated (rare).

Safe to run at any time regardless of current state. No need to stop first.

### Start/stop without rebuilding

```
./stop-exe.sh                # stop containers (keeps volumes)
./start-exe.sh               # start containers without rebuilding
```

Use `start-exe.sh` when you stopped the stack and just want to bring it back up with the same code.

**Note:** `start-exe.sh` only works if containers still exist (i.e. stopped with `stop-exe.sh`). After `stop-exe.sh -v` or `stop-db-exe.sh -v`, containers are removed - use `deploy-to-exe.sh` to bring everything back.

### Clean restart with fresh database

```
./stop-exe.sh                # stop everything
./stop-db-exe.sh -v          # wipe the DB volume
./deploy-to-exe.sh           # bring everything back up (Mongo creates empty db)
```

### Database only (start/stop independently)

```
./start-db-exe.sh            # start just MongoDB on VM
./stop-db-exe.sh             # stop just MongoDB on VM (keeps data)
./stop-db-exe.sh -v          # stop MongoDB and destroy volume (wipes DB!)
```

Useful when you want the shared DB running but are developing locally and don't need the VM app running.

### SSH tunnel for local dev

```
./db-tunnel.sh               # open tunnel in background (idempotent)
./db-tunnel.sh stop          # kill background tunnel
```

The tunnel forwards local `localhost:27017` to VM MongoDB over SSH. Your local app connects to it transparently.

---

## Backup and restore

The data (places + photos in GridFS) is valuable. `backup-db.sh` and `restore-db.sh` give you a single-file portable snapshot in `mongodump --archive --gzip` format - copy it anywhere (local disk, USB, external drive, cloud storage) and restore it into any MongoDB 8.x instance.

**Important: the tunnel is NOT required for backup/restore.** The scripts SSH into the VM and run `docker exec` on the mongo container directly. Dump/restore streams travel over the SSH channel itself. `db-tunnel.sh` is a separate feature only used when the local app *process* wants to talk to VM Mongo as if it were on `localhost:27017`.

### Backup (VM -> local file)

```
./backup-db.sh                             # -> ./backups/placestracker-YYYYMMDD-HHMMSS.archive.gz
./backup-db.sh /Volumes/MyUSB              # -> /Volumes/MyUSB/placestracker-...archive.gz
./backup-db.sh /tmp/snapshot.archive.gz    # -> exact path
```

What it does, end to end:
1. Reads `.deploy-config` for VM_HOST/VM_USER.
2. Opens SSH to the VM.
3. On the VM: `docker exec placestracker-mongodb mongodump --db=placestracker --archive --gzip`
4. mongodump writes to stdout inside the container; docker exec surfaces it; SSH streams it back.
5. Local shell redirects stdout to the output file.

Nothing is written on the VM's disk - the stream goes straight to your local file. A typical full backup of this dataset is ~1.4 GB (GridFS photos are already JPEG-compressed, so gzip barely shrinks them).

`backups/` is in `.gitignore`, so backups don't leak into commits.

### Restore (file -> target Mongo)

```
./restore-db.sh <file>                       # into LOCAL container (placestracker-mongodb)
./restore-db.sh <file> --vm                  # into VM container (SSH + docker exec)
./restore-db.sh <file> --uri <mongo-uri>     # into any Mongo reachable by URI
```

All three modes pass `--drop`, so existing collections are dropped before restoring. There's no half-merged state if something crashes mid-restore - you just re-run it.

- **Local mode** (default): requires `placestracker-mongodb` container running locally (`./compose-up.sh` or `./compose-up-standalone.sh`). Streams the file into `docker exec -i placestracker-mongodb mongorestore --gzip --archive --drop`.
- **`--vm` mode**: same thing but over SSH into the VM. No tunnel needed.
- **`--uri` mode**: requires `mongorestore` installed on your machine (`brew install mongodb-database-tools`). Hands the file and URI directly to `mongorestore` - use this for migrating to any third-party Mongo (Atlas, self-hosted, another VM, whatever).

### Manual restore (no script, any machine)

The backup file is self-describing. On any machine with MongoDB Database Tools installed:

```
mongorestore --gzip --archive=placestracker-20260411-090426.archive.gz --drop \
    --uri=mongodb://HOST:27017
```

Or into a running container without host tooling:

```
docker exec -i <mongo-container-name> mongorestore --gzip --archive --drop \
    < placestracker-20260411-090426.archive.gz
```

### Operational suggestions

- **Before any schema change or risky refactor**: `./backup-db.sh` first. It's a single SSH stream, takes a few minutes.
- **Rotating snapshots**: `./backup-db.sh` generates a timestamped filename every time, so repeated runs don't overwrite each other. Clean `backups/` periodically if disk fills up.
- **Off-machine copy**: after a backup, `cp` or `rsync` the file to a USB drive or cloud storage. The file is the atomic unit - one file, one snapshot.
- **Restoring into Atlas / a remote cluster**: use `--uri` mode with the cluster's SRV or standard connection string. Atlas accepts mongodump archives directly.

### All commands

```
./deploy-to-exe.sh           # full redeploy (pull + rebuild + restart)
./start-exe.sh               # start all containers without rebuild
./stop-exe.sh                # stop all containers (keeps volumes)
./stop-exe.sh -v             # stop and remove volumes (destroys DB!)
./start-db-exe.sh            # start just MongoDB
./stop-db-exe.sh             # stop just MongoDB (keeps data)
./stop-db-exe.sh -v          # stop MongoDB and destroy volume (wipes DB!)
./db-tunnel.sh               # SSH tunnel to VM MongoDB
./db-tunnel.sh stop          # kill the tunnel
./dev-remote-db.sh           # start VM DB + tunnel in one step
./compose-up-local.sh        # local app container + VM DB
./compose-up-local.sh -b     # build and start local app container + VM DB
./compose-down-local.sh      # stop local app container (tunnel stays)
./compose-down-local.sh -v   # stop and remove volumes
./status-exe.sh              # container status + recent logs
./logs-exe.sh                # tail all logs
./logs-exe.sh app            # tail app logs only
./logs-exe.sh mongodb        # tail db logs only
./backup-db.sh               # dump VM Mongo to ./backups/placestracker-TIMESTAMP.archive.gz
./backup-db.sh <dir|file>    # dump to the given directory or exact file path
./restore-db.sh <file>       # restore a backup into local container
./restore-db.sh <file> --vm  # restore a backup into VM container
./restore-db.sh <file> --uri <mongo-uri>   # restore into any Mongo URI
```

---

## File layout

```
places-tracker/
├── Dockerfile                    # local dev image (JDK, self-signed SSL, entrypoint generates cert)
├── Dockerfile.prod               # production image (JRE only, no SSL, healthcheck via /placestracker)
├── docker-compose.yml            # local-only: mongodb
├── docker-compose.standalone.yml # local: mongodb + app with self-signed cert
├── docker-compose.local.yml      # local container + VM DB (SSH tunnel)
├── docker-compose.prod.yml       # VM: mongodb + app, ports bound to 127.0.0.1
├── .deploy-config.example        # committed: VM host, user, app dir, repo URL
├── .deploy-config                # IGNORED: your values
├── .env.prod.example             # committed: env var template
├── .env.prod                     # IGNORED: Google Maps key etc.
├── deploy-to-exe.sh              # clone/pull, scp .env, build, up -d
├── start-exe.sh / stop-exe.sh    # whole-stack start/stop
├── start-db-exe.sh / stop-db-exe.sh # DB-only start/stop
├── db-tunnel.sh                  # SSH -L 27017 -> VM 27017
├── dev-remote-db.sh              # start VM DB + tunnel, stop local Mongo
├── compose-up-local.sh           # local container + VM DB (-b to build)
├── compose-down-local.sh         # stop local container
├── status-exe.sh                 # docker compose ps + last 30 lines
├── logs-exe.sh [app|mongodb]     # follow logs
├── backup-db.sh                  # VM mongo -> gzipped archive file (no tunnel needed)
├── restore-db.sh                 # archive file -> local | --vm | --uri target
└── backups/                      # IGNORED: generated backup files
```

---

## Troubleshooting

### `docker compose up` fails on the VM with a build error

`./deploy-to-exe.sh` does the build remotely. SSH in and check:

```
ssh exedev@your-app.exe.xyz
cd places-tracker
docker compose -f docker-compose.prod.yml build --no-cache app
```

### App starts but 502 from exe.dev

The exe.dev proxy needs the port forwarded. The deploy script runs:

```
ssh exe.dev share port places-tracker 8080
```

If that failed silently, re-run it manually.

### `./db-tunnel.sh` says port is already open

Something is already bound to local `:27017`. Usually a local Mongo container. Kill it:

```
docker compose -f docker-compose.standalone.yml down
docker compose -f docker-compose.yml down
./db-tunnel.sh
```

### Local app can't see data I saved on the VM app

Make sure both are using the same database. Local should be connecting via the tunnel:
- `dev-remote-db.sh` output says `Tunnel: already open` or `Tunnel started`
- `lsof -i :27017` shows an `ssh` process listening
- Local `SPRING_MONGODB_URI` (or default) is `mongodb://localhost:27017/placestracker`

If the local app is running inside a container, it must use `host.docker.internal:27017` (that's what `docker-compose.local.yml` does).

### MongoDB volume wipe didn't actually wipe

The volume name on the VM is `placestracker-prod_placestracker_mongodata` (compose project name + volume name). `stop-db-exe.sh -v` targets that name. If your project name differs, check:

```
ssh exedev@your-app.exe.xyz docker volume ls
```

And remove manually:

```
ssh exedev@your-app.exe.xyz docker volume rm <name>
```
