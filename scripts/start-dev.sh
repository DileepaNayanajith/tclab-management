#!/bin/sh

set -e

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is not installed. Install Maven, then run npm run dev again."
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "Node.js/npm is not installed. Install Node.js, then run npm run dev again."
  exit 1
fi

if [ ! -d "$PROJECT_DIR/frontend/node_modules" ]; then
  echo "Frontend packages are missing. Run: cd frontend && npm install"
  exit 1
fi

cleanup() {
  if [ -n "$BACKEND_PID" ]; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

if command -v curl >/dev/null 2>&1 && curl -fsS http://127.0.0.1:8080/api/health >/dev/null 2>&1; then
  echo "Spring Boot backend is already running at http://127.0.0.1:8080"
else
  echo "Starting Spring Boot backend at http://127.0.0.1:8080"
  (
    cd "$PROJECT_DIR/backend"
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
  ) &
  BACKEND_PID=$!
fi

echo "Starting React frontend at http://127.0.0.1:5173"
cd "$PROJECT_DIR/frontend"
npm run dev
