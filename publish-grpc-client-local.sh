#!/bin/bash

# Script to build and publish gRPC client to Maven Local
# Usage: ./publish-grpc-client-local.sh [version]
# Example: ./publish-grpc-client-local.sh 1.0.0

set -e

# Default version if not provided
VERSION="${1:-1.0.0}"

echo "=========================================="
echo "Publishing gRPC Client to Maven Local"
echo "Version: $VERSION"
echo "=========================================="

# Generate Proto sources
echo ""
echo "[1/3] Generating Proto sources..."
./gradlew generateProto

# Build gRPC Client JARs
echo ""
echo "[2/3] Building gRPC Client JAR..."
./gradlew grpcClientJar grpcClientSourcesJar -PgrpcClientVersion="$VERSION"

# Publish to Maven Local
echo ""
echo "[3/3] Publishing to Maven Local..."
./gradlew publishGrpcClientPublicationToMavenLocal -PgrpcClientVersion="$VERSION"

echo ""
echo "=========================================="
echo "Successfully published!"
echo ""
echo "Artifact: com.nekgamebling:game-grpc-client:$VERSION"
echo "Location: ~/.m2/repository/com/nekgamebling/game-grpc-client/$VERSION/"
echo ""
echo "Usage in build.gradle.kts:"
echo "  repositories {"
echo "      mavenLocal()"
echo "  }"
echo ""
echo "  dependencies {"
echo "      implementation(\"com.nekgamebling:game-grpc-client:$VERSION\")"
echo "  }"
echo "=========================================="
