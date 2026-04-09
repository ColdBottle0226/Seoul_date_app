// ============================================================
// MongoDB 초기화 스크립트
// date_app 데이터베이스 + 컬렉션 + 인덱스 생성
// ============================================================

// ── DB 전환 ──────────────────────────────────────────────
db = db.getSiblingDB('date_app');

// ============================================================
// Collection: places_detail
// ── place_id(MySQL place_db.places.id) 기반 상세 문서
// ── 서울 실시간 도시데이터 OA-21285 필드 직접 매핑
// ============================================================
db.createCollection('places_detail');

db.places_detail.createIndex({ place_id: 1 }, { unique: true, name: 'idx_place_id' });
db.places_detail.createIndex({ 'realtime.area_code': 1 }, { name: 'idx_area_code' });
db.places_detail.createIndex({ 'realtime.updated_at': 1 }, { expireAfterSeconds: 600, name: 'ttl_realtime' }); // 실시간 필드 TTL 10분
db.places_detail.createIndex({ atmosphere_tags: 1 }, { name: 'idx_atmosphere_tags' });
db.places_detail.createIndex({ food_tags: 1 }, { name: 'idx_food_tags' });
db.places_detail.createIndex({ updated_at: -1 }, { name: 'idx_updated_at' });

// 샘플 도큐먼트 구조 설명용 더미 (운영에서는 삭제)
db.places_detail.insertOne({
  place_id: NumberLong(0),  // 예약 ID — 실제 데이터 아님
  _placeholder: true,
  description: '',
  short_description: '',
  image_keys: [],
  menus: [],
  price_range: { min: 0, max: 0, currency: 'KRW' },
  facilities: {
    parking: false, wifi: false, pet_friendly: false,
    kids_friendly: false, wheelchair_accessible: false,
    valet_parking: false, delivery: false, takeout: false
  },
  atmosphere_tags: [],
  food_tags: [],
  // 서울 실시간 도시데이터 OA-21285 응답 필드 매핑
  realtime: {
    area_code: '',           // AREA_CD
    area_name: '',           // AREA_NM
    congestion_level: '',    // AREA_CONGEST_LVL (여유/보통/약간붐빔/붐빔)
    congestion_message: '',  // AREA_CONGEST_MSG
    population: { min: 0, max: 0 },  // AREA_PPLTN_MIN / AREA_PPLTN_MAX
    population_forecast: [], // FCST_PPLTN 배열
    weather: {
      measured_at: new Date(),
      temp: null,            // TEMP
      sensible_temp: null,   // SENSIBLE_TEMP
      max_temp: null,        // MAX_TEMP
      min_temp: null,        // MIN_TEMP
      humidity: null,        // HUMIDITY
      wind_direction: '',    // WIND_DIRCT
      wind_speed: null,      // WIND_SPD
      precipitation: null,   // PRECIPITATION
      precpt_type: '',       // PRECPT_TYPE (없음/비/눈/비또는눈)
      uv_index_level: '',    // UV_INDEX_LVL
      pm10_index: '',        // PM10_INDEX
      pm25_index: '',        // PM25_INDEX
      air_index: ''          // AIR_IDX
    },
    road_traffic: {
      index: '',             // ROAD_TRAFFIC_IDX (원활/서행/정체)
      message: '',           // ROAD_MSG
      avg_speed_kmh: null    // ROAD_TRAFFIC_SPD
    },
    parking: { available_count: 0, total_count: 0 },
    nearby_bikes: [],
    updated_at: new Date()
  },
  updated_at: new Date()
});

// ============================================================
// Collection: review_details
// ── feedbacks(MySQL)와 review_id 매핑
// ── 이미지 키, 항목별 평점 등 비정형 데이터
// ============================================================
db.createCollection('review_details');

db.review_details.createIndex({ feedback_id: 1 }, { unique: true, name: 'idx_feedback_id' });
db.review_details.createIndex({ place_id: 1 }, { name: 'idx_place_id' });
db.review_details.createIndex({ user_id: 1 }, { name: 'idx_user_id' });
db.review_details.createIndex({ created_at: -1 }, { name: 'idx_created_at' });

// ============================================================
// Collection: user_activity_logs
// ── 사용자 행동 로그 (추천 개인화용)
// ── TTL: 90일 자동 삭제
// ============================================================
db.createCollection('user_activity_logs');

db.user_activity_logs.createIndex({ user_id: 1, created_at: -1 }, { name: 'idx_user_activity' });
db.user_activity_logs.createIndex({ target_id: 1, target_type: 1 }, { name: 'idx_target' });
db.user_activity_logs.createIndex({ created_at: 1 }, { expireAfterSeconds: 7776000, name: 'ttl_90days' }); // 90일

// ============================================================
// Collection: date_course_histories
// ── date_courses(MySQL)와 course_id 매핑
// ── LLM 프롬프트 히스토리, RAG 컨텍스트
// ============================================================
db.createCollection('date_course_histories');

db.date_course_histories.createIndex({ course_id: 1 }, { unique: true, name: 'idx_course_id' });
db.date_course_histories.createIndex({ user_id: 1, created_at: -1 }, { name: 'idx_user_history' });

// ============================================================
// Collection: seoul_realtime_snapshots
// ── 실시간 데이터 스냅샷 (5분 단위 저장, 분석용)
// ── TTL: 48시간 자동 삭제
// ============================================================
db.createCollection('seoul_realtime_snapshots');

db.seoul_realtime_snapshots.createIndex({ area_code: 1, captured_at: -1 }, { name: 'idx_area_time' });
db.seoul_realtime_snapshots.createIndex({ captured_at: 1 }, { expireAfterSeconds: 172800, name: 'ttl_48h' });

print('✅ MongoDB 초기화 완료: date_app DB, 5개 컬렉션 및 인덱스 생성');
