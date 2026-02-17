#!/usr/bin/env bash
# ──────────────────────────────────────────────
# deploy.sh — Pull latest images and redeploy
# Usage:  ./scripts/deploy.sh [IMAGE_TAG]
# ──────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
IMAGE_TAG="${1:-latest}"

cd "$PROJECT_DIR"

echo "╔══════════════════════════════════════════╗"
echo "║   Employee Management System Deploy      ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── Pre-checks ────────────────────────────────
if ! command -v docker &>/dev/null; then
  echo "❌ Docker is not installed. Please install Docker first." >&2
  exit 1
fi

if [ ! -f .env ]; then
  echo "❌ .env file not found. Copy .env.example to .env and configure it." >&2
  exit 1
fi

echo "📋 Image tag: $IMAGE_TAG"
echo ""

# ── Pull latest images ────────────────────────
echo "⬇️  Pulling images..."
IMAGE_TAG="$IMAGE_TAG" docker compose pull ems-backend ems-frontend
echo ""

# ── Deploy ────────────────────────────────────
echo "🚀 Starting services..."
IMAGE_TAG="$IMAGE_TAG" docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  up -d
echo ""

# ── Health check ──────────────────────────────
echo "🩺 Waiting for services to be healthy..."
MAX_WAIT=120
INTERVAL=3
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
  BACKEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/ 2>/dev/null || echo "000")
  FRONTEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:80/ 2>/dev/null || echo "000")

  if [ "$BACKEND_CODE" != "000" ] && [ "$FRONTEND_CODE" != "000" ]; then
    echo ""
    echo "✅ Backend  → HTTP $BACKEND_CODE"
    echo "✅ Frontend → HTTP $FRONTEND_CODE"
    break
  fi

  printf "."
  sleep $INTERVAL
  ELAPSED=$((ELAPSED + INTERVAL))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
  echo ""
  echo "⚠️  Services may not be fully healthy yet."
  echo "Check logs: docker compose logs -f"
fi

echo ""
echo "📊 Service status:"
docker compose ps
echo ""
echo "🎉 Deployment complete!"
