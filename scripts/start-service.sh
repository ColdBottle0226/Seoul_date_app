#!/bin/bash

# ============================================================
# Seoul Date App — 서비스 실행 스크립트 (환경 변수 자동 로드)
# ============================================================

SERVICE_NAME=$1
PROJECT_ROOT=$(pwd)
DOTENV_PATH="$PROJECT_ROOT/.env"

if [ -z "$SERVICE_NAME" ]; then
    echo "사용법: ./scripts/start-service.sh [service-name]"
    echo "예: ./scripts/start-service.sh user-service"
    exit 1
fi

# 1. .env 파일 로드
if [ -f "$DOTENV_PATH" ]; then
    echo "파일 로드 중: $DOTENV_PATH"
    # 주석 및 빈 줄 제외하고 export
    export $(grep -v '^#' "$DOTENV_PATH" | xargs)
else
    echo "경고: .env 파일을 찾을 수 없습니다 ($DOTENV_PATH)"
fi

# 2. 서비스 실행 (루트의 gradlew 사용)
SERVICE_DIR="$PROJECT_ROOT/services/$SERVICE_NAME"
if [ -d "$SERVICE_DIR" ]; then
    echo "서비스 시작 중: $SERVICE_NAME"
    # 루트의 gradlew를 사용하여 해당 모듈의 bootRun 실행
    ./gradlew ":services:$SERVICE_NAME:bootRun"
else
    echo "오류: 서비스 디렉토리를 찾을 수 없습니다 ($SERVICE_DIR)"
    exit 1
fi
