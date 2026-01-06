#!/bin/bash

# Local build script for game-core
# Creates release distribution for Docker

set -e

echo "========================================"
echo "Building game-core release"
echo "========================================"

# Clean and build
./gradlew clean build -x test

echo ""
echo "========================================"
echo "Build completed!"
echo "========================================"
echo "Distribution: build/distributions/game-core-1.0.0.tar"
echo ""
echo "Build Docker image:"
echo "  docker build -t game-core ."
echo ""
