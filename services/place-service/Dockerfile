# place-service Dockerfile (NestJS)
# Multi-stage build
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production && npm ci

COPY . .
RUN npm run build

# ── Production stage ──────────────────────────────────────────
FROM node:20-alpine AS production

WORKDIR /app

# sharp는 native 모듈이므로 alpine 호환 버전 설치
RUN apk add --no-cache vips-dev

COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/package*.json ./

# OpenTelemetry 트레이싱을 main.js 이전에 초기화하기 위해 --require 사용
ENV NODE_PATH=/app/node_modules

EXPOSE 8082

CMD ["node", "--require", "./dist/tracing/tracing.js", "dist/main"]
