#!/bin/bash

# ============================================================
# Seoul Date App — MSA 및 Gateway 통합 테스트 스크립트
# ============================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== MSA 구성 및 Gateway 작동 테스트 시작 ===${NC}"

# 1. 인프라 상태 확인
echo -e "\n${BLUE}[1/3] 인프라 기본 상태 점검 (Health Check)${NC}"

check_health() {
    local name=$1
    local url=$2
    local status=$(curl -s $url | grep -o 'UP' | head -1)
    if [ "$status" == "UP" ]; then
        echo -e "${GREEN}[SUCCESS] $name is UP${NC}"
    else
        echo -e "${RED}[FAILURE] $name is NOT UP (Status: $status)${NC}"
    fi
}

check_health "Eureka Server" "http://localhost:8761/actuator/health"
check_health "Config Server" "http://localhost:8888/actuator/health"
check_health "API Gateway  " "http://localhost:8080/actuator/health"

# 2. Config Server 설정 배포 확인
echo -e "\n${BLUE}[2/3] Config Server 설정 배포 확인${NC}"
config_check=$(curl -s http://localhost:8888/user-service/default | grep "propertySources")
if [[ -n "$config_check" ]]; then
    echo -e "${GREEN}[SUCCESS] Config Server가 설정을 정상적으로 배포하고 있습니다.${NC}"
else
    echo -e "${RED}[FAILURE] Config Server에서 설정을 찾을 수 없습니다.${NC}"
fi

# 3. Gateway 라우팅 및 동적 경로 확인
echo -e "\n${BLUE}[3/3] Gateway 라우팅 설정 점검${NC}"
routes=$(curl -s http://localhost:8080/actuator/gateway/routes)
if [[ -n "$routes" ]]; then
    echo -e "${GREEN}[SUCCESS] Gateway 라우팅 목록을 불러왔습니다.${NC}"
    echo -e "현재 등록된 라우팅 ID:"
    echo "$routes" | grep -o '"route_id":"[^"]*"' | cut -d'"' -f4 | sed 's/^/ - /'
else
    echo -e "${RED}[FAILURE] Gateway 라우팅 정보를 가져올 수 없습니다.${NC}"
fi

echo -e "\n${BLUE}=== 테스트 완료 ===${NC}"
echo -e "모든 인프라가 UP 상태라면, 이제 비즈니스 서비스를 실행하여 실제 API 호출을 테스트할 수 있습니다."
