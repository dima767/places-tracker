#!/bin/bash

# Parse command line arguments
BUILD=false
RERUN=false
while getopts "br" opt; do
    case $opt in
        b)
            BUILD=true
            ;;
        r)
            RERUN=true
            ;;
        \?)
            echo "Usage: $0 [-b] [-r]"
            echo "  -b    Clean build before running"
            echo "  -r    Force rerun tasks (ignore up-to-date checks)"
            exit 1
            ;;
    esac
done

# Build if -b flag is provided
if [ "$BUILD" = true ]; then
    echo "=== Building Places Tracker ==="

    GRADLE_ARGS="clean build"
    if [ "$RERUN" = true ]; then
        GRADLE_ARGS="$GRADLE_ARGS --rerun-tasks"
        echo "Force rerunning all tasks..."
    fi

    ./gradlew $GRADLE_ARGS

    if [ $? -ne 0 ]; then
        echo ""
        echo "=== Build failed! Please fix errors and try again ==="
        exit 1
    fi
    echo ""
fi

# Find the JAR file (exclude -plain.jar if it exists)
JAR_FILE=$(find build/libs -name "*.jar" ! -name "*-plain.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Could not find JAR file in build/libs/"
    echo "Tip: Run with -b flag to build first: $0 -b"
    exit 1
fi

echo "=== Starting application with remote debugging on port 5005 ==="
echo "Running: $JAR_FILE"
echo ""

java \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.nio=ALL-UNNAMED \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
    $JAVA_OPTS \
    -jar "$JAR_FILE"
