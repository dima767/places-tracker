# exe.dev Deployment Guide for Spring Boot Applications

Complete guide for deploying Spring Boot applications to exe.dev VMs using Docker and nginx.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Project Setup](#project-setup)
- [Docker Configuration](#docker-configuration)
- [Deployment Scripts](#deployment-scripts)
- [Nginx Configuration](#nginx-configuration)
- [Troubleshooting](#troubleshooting)
- [Spring Boot Version Notes](#spring-boot-version-notes)

## Prerequisites

1. **exe.dev VM** - Create via `ssh exe.dev create <vm-name>`
2. **GitHub Repository** - Private or public repo for your Spring Boot app
3. **SSH Keys** - For GitHub authentication (deploy keys recommended for private repos)
4. **Docker** - Pre-installed on exe.dev VMs
5. **nginx** - Pre-installed, needs to be enabled

## Project Setup

### 1. Create Dockerfile

```dockerfile
# Multi-stage build for Spring Boot application
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || return 0

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port 8080 (standard for Spring Boot in Docker)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=docker"]
```

### 2. Create docker-compose.prod.yml

**Key Points:**
- Use port 8080 for the app container
- Set context path to `/yourappname`
- MongoDB uses service name for hostname (not localhost)
- Use environment variables for configuration

```yaml
version: '3.8'

services:
  # MongoDB Database (if needed)
  mongodb:
    image: mongo:8.0
    container_name: yourapp-mongodb
    restart: unless-stopped
    environment:
      MONGO_INITDB_DATABASE: yourapp
    volumes:
      - mongodb_data:/data/db
      - mongodb_config:/data/configdb
    networks:
      - yourapp-network
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh localhost:27017/yourapp --quiet
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 40s

  # Your Spring Boot Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: yourapp:latest
    container_name: yourapp-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      # MongoDB connection (Spring Boot 4.0 uses spring.mongodb.uri)
      # For older versions use: spring.data.mongodb.uri
      SPRING_MONGODB_URI: mongodb://mongodb:27017/yourapp

      # Server configuration
      SERVER_SSL_ENABLED: false
      SERVER_PORT: 8080
      SERVER_SERVLET_CONTEXT_PATH: /yourappname

      # Add your app-specific environment variables
      YOUR_API_KEY: ${YOUR_API_KEY}

      # Java memory settings
      JAVA_OPTS: "-Xms512m -Xmx1g"
    volumes:
      - app_logs:/app/logs
    depends_on:
      mongodb:
        condition: service_healthy
    networks:
      - yourapp-network

volumes:
  mongodb_data:
    driver: local
  mongodb_config:
    driver: local
  app_logs:
    driver: local

networks:
  yourapp-network:
    driver: bridge
```

### 3. Create .dockerignore

```
# Gradle
.gradle/
build/

# IDE
.idea/
*.iws
*.iml
*.ipr
out/

# OS
.DS_Store

# Logs
logs/
*.log

# Environment
.env
.deploy-config

# Git
.git/
.gitignore
```

### 4. Update application.properties for Docker

**Spring Boot 4.0+ (uses `spring.mongodb.*`):**
```properties
# MongoDB (Spring Boot 4.0+)
spring.mongodb.uri=${SPRING_MONGODB_URI:mongodb://localhost:27017/yourapp}

# Server
server.port=8443
server.servlet.context-path=/yourappname

# SSL for local dev only
server.ssl.enabled=true
server.ssl.key-store=classpath:ssl/keystore.p12
server.ssl.key-store-password=changeit
```

**Spring Boot 3.x (uses `spring.data.mongodb.*`):**
```properties
# MongoDB (Spring Boot 3.x)
spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/yourapp}

# Rest is the same...
```

## Deployment Scripts

### 1. Create deploy-to-exe.sh

Copy and adapt from `deploy-to-exe.sh` in this repo. Key customizations:

```bash
# Update these variables
VM_HOST="yourvm.exe.xyz"        # Your exe.dev VM hostname
REPO_URL=""                      # Your GitHub repo URL
CONTEXT_PATH="/yourappname"      # Your app's context path
```

### 2. Create .deploy-config

```bash
# Copy example and customize
cp .deploy-config.example .deploy-config

# Edit with your values
nano .deploy-config
```

Example `.deploy-config`:
```bash
# Git Repository (SSH URL with deploy key)
REPO_URL=git@github.com:username/yourapp.git

# Deploy Key Path (for private repos)
DEPLOY_KEY_PATH=~/.ssh/yourapp-deploy-key

# Your app-specific secrets
YOUR_API_KEY=your-api-key-here

# VM Configuration
VM_HOST=yourvm.exe.xyz
VM_USER=execdev
```

### 3. Create .deploy-config.example

```bash
# Git Repository URL
REPO_URL=git@github.com:username/yourapp.git

# Deploy Key Path (optional - for private repos)
DEPLOY_KEY_PATH=~/.ssh/yourapp-deploy-key

# Your app-specific environment variables
YOUR_API_KEY=your-api-key-here

# VM Configuration (exe.dev)
VM_HOST=yourvm.exe.xyz
VM_USER=execdev
```

### 4. Add to .gitignore

```
# Deployment configuration (contains secrets)
.deploy-config
```

## Nginx Configuration

### Automated Setup (Recommended)

Use the provided `setup-nginx.sh` script to automate nginx configuration:

```bash
# One-time setup (uses .deploy-config values)
./setup-nginx.sh

# Or specify context path
./setup-nginx.sh /yourappname
```

The script will:
- ✅ Enable nginx if not running
- ✅ Create nginx site configuration with port 8000 support
- ✅ Enable the site
- ✅ Test configuration
- ✅ Reload nginx
- ✅ Verify setup

### Manual Setup (Alternative)

If you prefer manual configuration:

#### 1. Enable nginx on exe.dev VM

```bash
ssh execdev@yourvm.exe.xyz sudo systemctl enable --now nginx
```

#### 2. Create nginx site configuration

**CRITICAL:** exe.dev routes HTTPS traffic to port 8000, so nginx must listen on both port 80 AND port 8000!

```bash
ssh execdev@yourvm.exe.xyz "sudo tee /etc/nginx/sites-available/yourapp > /dev/null << 'EOF'
server {
    listen 80;
    listen [::]:80;
    listen 8000;
    listen [::]:8000;
    server_name yourvm.exe.xyz;

    location /yourappname {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF
"
```

#### 3. Enable site and reload nginx

```bash
ssh execdev@yourvm.exe.xyz "
  sudo ln -sf /etc/nginx/sites-available/yourapp /etc/nginx/sites-enabled/yourapp &&
  sudo nginx -t &&
  sudo systemctl reload nginx
"
```

## GitHub Deploy Keys (for Private Repos)

### 1. Generate SSH key locally

```bash
ssh-keygen -t ed25519 -f ~/.ssh/yourapp-deploy-key -N "" -C "yourapp-deploy-key"
```

### 2. Add public key to GitHub

```bash
# Display public key
cat ~/.ssh/yourapp-deploy-key.pub
```

- Go to: `https://github.com/username/yourapp/settings/keys`
- Click "Add deploy key"
- Title: `exe.dev deployment`
- Paste public key
- **DO NOT** check "Allow write access"
- Click "Add key"

### 3. Update .deploy-config

```bash
REPO_URL=git@github.com:username/yourapp.git
DEPLOY_KEY_PATH=~/.ssh/yourapp-deploy-key
```

The deployment script will automatically copy the key to the VM and configure SSH.

## Deployment Workflow

### Initial Deployment

```bash
# 1. Ensure all config files are ready
ls -la .deploy-config Dockerfile docker-compose.prod.yml

# 2. Run deployment script
./deploy-to-exe.sh

# 3. Configure nginx (one-time)
./setup-nginx.sh

# 4. Test
curl https://yourvm.exe.xyz/yourappname/
```

### Subsequent Deployments

```bash
# Just run the deployment script
./deploy-to-exe.sh
```

The script will:
1. Update code via git pull
2. Rebuild Docker image with --no-cache
3. Restart containers
4. Verify deployment

## Troubleshooting

### MongoDB Connection Issues

**Problem:** App can't connect to MongoDB

**Solutions:**

1. **Check Spring Boot version:**
   - Spring Boot 4.0+: Use `spring.mongodb.uri`
   - Spring Boot 3.x: Use `spring.data.mongodb.uri`

2. **Verify environment variable:**
   ```bash
   docker compose -f docker-compose.prod.yml exec app env | grep MONGO
   ```

3. **Check MongoDB is running:**
   ```bash
   docker compose -f docker-compose.prod.yml ps
   docker compose -f docker-compose.prod.yml logs mongodb
   ```

4. **Test connection:**
   ```bash
   docker compose -f docker-compose.prod.yml exec app sh -c \
     'curl -s http://localhost:8080/yourappname/actuator/health'
   ```

### Nginx 404 Errors

**Problem:** `https://yourvm.exe.xyz/yourappname` returns 404

**Solutions:**

1. **Verify nginx is listening on port 8000:**
   ```bash
   ssh execdev@yourvm.exe.xyz "sudo netstat -tlnp | grep nginx"
   # Should show both :80 and :8000
   ```

2. **Check nginx configuration:**
   ```bash
   ssh execdev@yourvm.exe.xyz "sudo nginx -T | grep -A 10 'location /yourappname'"
   ```

3. **Test app directly:**
   ```bash
   ssh execdev@yourvm.exe.xyz "curl -v http://localhost:8080/yourappname/"
   ```

4. **Check nginx logs:**
   ```bash
   ssh execdev@yourvm.exe.xyz "sudo tail -f /var/log/nginx/error.log"
   ```

### Docker Build Cache Issues

**Problem:** Code changes not reflected after deployment

**Solution:**
```bash
# The deploy script uses --no-cache, but if issues persist:
ssh execdev@yourvm.exe.xyz "
  cd yourapp &&
  docker compose -f docker-compose.prod.yml down &&
  docker system prune -af &&
  docker compose -f docker-compose.prod.yml up -d --build
"
```

### Port Conflicts

**Problem:** Port 8080 already in use

**Solutions:**

1. **Find conflicting process:**
   ```bash
   ssh execdev@yourvm.exe.xyz "sudo lsof -i :8080"
   ```

2. **Change app port in docker-compose.prod.yml:**
   ```yaml
   ports:
     - "8081:8080"  # Use different host port
   ```

3. **Update nginx proxy_pass:**
   ```nginx
   proxy_pass http://localhost:8081;
   ```

## Spring Boot Version Notes

### Spring Boot 4.0+ (Spring Framework 7.0)

**MongoDB Configuration Changes:**
- Old: `spring.data.mongodb.*`
- New: `spring.mongodb.*`

**application.properties:**
```properties
spring.mongodb.uri=${SPRING_MONGODB_URI:mongodb://localhost:27017/yourapp}
```

**docker-compose.prod.yml:**
```yaml
environment:
  SPRING_MONGODB_URI: mongodb://mongodb:27017/yourapp
```

### Spring Boot 3.x (Spring Framework 6.0)

**MongoDB Configuration:**
```properties
spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/yourapp}
```

**docker-compose.prod.yml:**
```yaml
environment:
  SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/yourapp
```

## Quick Reference Checklist

- [ ] Dockerfile created
- [ ] docker-compose.prod.yml configured
- [ ] .dockerignore added
- [ ] application.properties updated for Docker
- [ ] deploy-to-exe.sh script copied and customized
- [ ] .deploy-config created (not committed to git)
- [ ] .deploy-config.example created
- [ ] .gitignore updated to exclude .deploy-config
- [ ] Deploy key generated (for private repos)
- [ ] Deploy key added to GitHub
- [ ] VM created on exe.dev
- [ ] nginx enabled on VM
- [ ] nginx site configuration created
- [ ] nginx listening on port 8000 ✅ **CRITICAL**
- [ ] Initial deployment successful
- [ ] Application accessible via HTTPS

## Additional Management Scripts

Consider creating these helper scripts:

**status-exe.sh** - Check application status
**stop-exe.sh** - Stop containers (keep data)
**destroy-exe.sh** - Complete cleanup
**logs-exe.sh** - View application logs

See this repo's scripts for examples.

## Security Best Practices

1. **Never commit secrets** - Use .deploy-config (git-ignored)
2. **Use deploy keys** - Not personal SSH keys
3. **Read-only deploy keys** - No write access needed
4. **Environment variables** - For all secrets in docker-compose
5. **Non-root containers** - Always create app user in Dockerfile
6. **Regular updates** - Keep base images and dependencies updated

## Resources

- [exe.dev Documentation](https://exe.dev)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [nginx Reverse Proxy Guide](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)
- [Spring Boot 4.0 Documentation](https://docs.spring.io/spring-boot/reference/)

## Example Repository

This repository (`park-tracker`) serves as a complete working example with:
- All configuration files
- Deployment scripts
- nginx setup
- MongoDB integration
- Spring Boot 4.0 configuration

Use it as a template for your own applications!
