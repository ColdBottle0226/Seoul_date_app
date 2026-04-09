#!/usr/bin/env bash
# ============================================================
# backup-to-gdrive.sh
# Docker 컨테이너 데이터를 덤프하고 구글 드라이브에 백업
#
# 사전 준비:
#   brew install rclone
#   rclone config  → 'gdrive' 이름으로 Google Drive 설정
#   (설정 가이드: scripts/docs/rclone-setup.md 참고)
#
# 사용법:
#   ./scripts/backup-to-gdrive.sh           # 전체 백업
#   ./scripts/backup-to-gdrive.sh mysql     # MySQL만
#   ./scripts/backup-to-gdrive.sh mongo     # MongoDB만
#   ./scripts/backup-to-gdrive.sh minio     # MinIO 이미지만
# ============================================================

set -euo pipefail

# ---- 설정 로드 ----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# .env 로드
if [ -f "$PROJECT_ROOT/.env" ]; then
  export $(grep -v '^#' "$PROJECT_ROOT/.env" | grep -v '^\s*$' | xargs)
else
  echo "❌ .env 파일이 없습니다. $PROJECT_ROOT/.env 를 확인하세요."
  exit 1
fi

# gdrive 설정 파일 로드 (.gdrive.conf)
GDRIVE_CONF="$PROJECT_ROOT/.gdrive.conf"
if [ -f "$GDRIVE_CONF" ]; then
  export $(grep -v '^#' "$GDRIVE_CONF" | grep -v '^\s*$' | xargs)
else
  echo "❌ .gdrive.conf 파일이 없습니다."
  echo "   cp $PROJECT_ROOT/.gdrive.conf.example $PROJECT_ROOT/.gdrive.conf 후 설정하세요."
  exit 1
fi

# rclone 설치 확인
if ! command -v rclone &> /dev/null; then
  echo "❌ rclone이 설치되어 있지 않습니다."
  echo "   brew install rclone"
  exit 1
fi

# ---- 변수 ----
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/tmp/seoul-date-backup-$TIMESTAMP"
GDRIVE_REMOTE="${GDRIVE_REMOTE_NAME:-gdrive}"        # rclone remote 이름
GDRIVE_PATH="${GDRIVE_BACKUP_PATH:-seoul-date-app/backups}"  # 드라이브 내 경로
TARGET="${1:-all}"

mkdir -p "$BACKUP_DIR"

echo "=============================="
echo " Seoul Date App 백업 시작"
echo " 대상: $TARGET"
echo " 타임스탬프: $TIMESTAMP"
echo "=============================="

# ============================================================
# MySQL 백업
# ============================================================
backup_mysql() {
  echo ""
  echo "📦 MySQL 백업 중..."
  local mysql_dir="$BACKUP_DIR/mysql"
  mkdir -p "$mysql_dir"

  for db_info in "date-mysql-user:3306:user_db" "date-mysql-place:3306:place_db" "date-mysql-rec:3306:recommendation_db"; do
    IFS=':' read -r container port dbname <<< "$db_info"

    echo "  → $dbname 덤프 중..."
    docker exec "$container" mysqldump \
      -u root \
      -p"${MYSQL_ROOT_PASSWORD}" \
      --single-transaction \
      --routines \
      --triggers \
      --hex-blob \
      "$dbname" > "$mysql_dir/${dbname}_${TIMESTAMP}.sql"

    # gzip 압축
    gzip "$mysql_dir/${dbname}_${TIMESTAMP}.sql"
    echo "  ✅ $dbname → ${dbname}_${TIMESTAMP}.sql.gz ($(du -sh "$mysql_dir/${dbname}_${TIMESTAMP}.sql.gz" | cut -f1))"
  done
}

# ============================================================
# MongoDB 백업
# ============================================================
backup_mongo() {
  echo ""
  echo "📦 MongoDB 백업 중..."
  local mongo_dir="$BACKUP_DIR/mongodb"
  mkdir -p "$mongo_dir"

  docker exec date-mongodb mongodump \
    --username "${MONGO_INITDB_ROOT_USERNAME}" \
    --password "${MONGO_INITDB_ROOT_PASSWORD}" \
    --authenticationDatabase admin \
    --db date_app \
    --archive \
    --gzip \
    > "$mongo_dir/date_app_${TIMESTAMP}.archive.gz"

  echo "  ✅ MongoDB → date_app_${TIMESTAMP}.archive.gz ($(du -sh "$mongo_dir/date_app_${TIMESTAMP}.archive.gz" | cut -f1))"
}

# ============================================================
# Redis 백업 (RDB 스냅샷 복사)
# ============================================================
backup_redis() {
  echo ""
  echo "📦 Redis 백업 중..."
  local redis_dir="$BACKUP_DIR/redis"
  mkdir -p "$redis_dir"

  # BGSAVE 트리거 후 dump.rdb 복사
  docker exec date-redis redis-cli -a "${REDIS_PASSWORD}" BGSAVE
  sleep 2  # 스냅샷 완료 대기

  docker cp date-redis:/data/dump.rdb "$redis_dir/dump_${TIMESTAMP}.rdb"
  gzip "$redis_dir/dump_${TIMESTAMP}.rdb"
  echo "  ✅ Redis → dump_${TIMESTAMP}.rdb.gz"
}

# ============================================================
# MinIO 이미지 백업 (rclone으로 직접 동기화)
# ============================================================
backup_minio() {
  echo ""
  echo "📦 MinIO 이미지 백업 중 (구글 드라이브로 직접 동기화)..."

  # MinIO를 rclone remote로 등록해서 GDrive로 직접 복사
  # (중간 임시 파일 없이 MinIO → GDrive 스트리밍)
  for bucket in places reviews events users; do
    echo "  → $bucket 버킷 동기화 중..."
    rclone sync \
      ":s3,provider=Minio,endpoint=http://localhost:9000,access_key_id=${MINIO_ROOT_USER},secret_access_key=${MINIO_ROOT_PASSWORD}:$bucket" \
      "${GDRIVE_REMOTE}:${GDRIVE_PATH}/minio/$bucket" \
      --progress \
      --transfers 4 \
      --checkers 8 \
      2>&1 | tail -3
    echo "  ✅ $bucket 버킷 동기화 완료"
  done
}

# ============================================================
# Elasticsearch 인덱스 스냅샷
# ============================================================
backup_elasticsearch() {
  echo ""
  echo "📦 Elasticsearch 스냅샷 백업 중..."
  local es_dir="$BACKUP_DIR/elasticsearch"
  mkdir -p "$es_dir"

  # places 인덱스 데이터만 JSON으로 export (로컬 개발 용)
  curl -s -u "elastic:${ELASTIC_PASSWORD}" \
    "http://localhost:9200/places/_search?size=10000&scroll=1m" \
    -H 'Content-Type: application/json' \
    -d '{"query":{"match_all":{}}}' \
    | gzip > "$es_dir/places_index_${TIMESTAMP}.json.gz"

  echo "  ✅ Elasticsearch → places_index_${TIMESTAMP}.json.gz"
}

# ============================================================
# 구글 드라이브에 업로드
# ============================================================
upload_to_gdrive() {
  echo ""
  echo "☁️  구글 드라이브 업로드 중..."
  echo "   대상: ${GDRIVE_REMOTE}:${GDRIVE_PATH}/"

  rclone copy "$BACKUP_DIR" \
    "${GDRIVE_REMOTE}:${GDRIVE_PATH}/" \
    --progress \
    --transfers 4

  echo ""
  echo "✅ 업로드 완료!"
  echo "   구글 드라이브 경로: ${GDRIVE_PATH}/"
}

# ============================================================
# 오래된 백업 정리 (구글 드라이브에서 30일 이상 된 파일 삭제)
# ============================================================
cleanup_old_backups() {
  echo ""
  echo "🧹 30일 이상 된 백업 정리 중..."
  rclone delete \
    "${GDRIVE_REMOTE}:${GDRIVE_PATH}/" \
    --min-age 30d \
    --rmdirs 2>/dev/null || true
  echo "  ✅ 정리 완료"
}

# ============================================================
# 실행
# ============================================================
case "$TARGET" in
  mysql)  backup_mysql;  upload_to_gdrive ;;
  mongo)  backup_mongo;  upload_to_gdrive ;;
  redis)  backup_redis;  upload_to_gdrive ;;
  minio)  backup_minio ;;  # minio는 직접 동기화라 upload 불필요
  es)     backup_elasticsearch; upload_to_gdrive ;;
  all)
    backup_mysql
    backup_mongo
    backup_redis
    backup_elasticsearch
    upload_to_gdrive
    backup_minio   # 마지막 (직접 동기화)
    cleanup_old_backups
    ;;
  *)
    echo "❌ 알 수 없는 대상: $TARGET"
    echo "   사용법: $0 [all|mysql|mongo|redis|minio|es]"
    exit 1
    ;;
esac

# 임시 파일 정리
rm -rf "$BACKUP_DIR"

echo ""
echo "=============================="
echo " ✅ 백업 완료: $TIMESTAMP"
echo "=============================="
