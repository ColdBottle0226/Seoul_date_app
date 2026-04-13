# Seoul Date App — API 설계서

**문서 버전**: v1.0.0
**작성일**: 2026-04-13
**작성자**: 개발팀
**Base URL**: `https://api.seouldate.app` (프로덕션) / `http://localhost:8080` (로컬)

---

## 목차

1. [공통 규칙](#1-공통-규칙)
2. [인증 API — /api/auth](#2-인증-api--apiauth)
3. [사용자 API — /api/users](#3-사용자-api--apiusers)
4. [내부 통신 API — /api/internal](#4-내부-통신-api--apiinternal)
5. [에러 코드 정의](#5-에러-코드-정의)
6. [보안 정책](#6-보안-정책)

---

## 1. 공통 규칙

### 1-1. 요청 공통 헤더

| 헤더 | 필수 | 설명 |
|---|---|---|
| `Content-Type` | 필수 | `application/json` |
| `Authorization` | 인증 필요 API | `Bearer {accessToken}` |
| `X-Request-Id` | 권장 | 클라이언트 요청 추적 ID (UUID v4) |

### 1-2. 응답 공통 포맷

**성공 응답**
```json
{
  "success": true,
  "data": { },
  "message": "처리되었습니다."
}
```

**페이지네이션 응답**
```json
{
  "success": true,
  "data": {
    "content": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true
  }
}
```

**실패 응답**
```json
{
  "success": false,
  "code": "USR_001",
  "message": "이미 사용 중인 이메일입니다.",
  "data": null
}
```

### 1-3. HTTP 상태 코드 사용 기준

| 코드 | 의미 | 사용 상황 |
|---|---|---|
| `200` | OK | 조회·수정 성공 |
| `201` | Created | 리소스 생성 성공 |
| `204` | No Content | 삭제 성공 (응답 바디 없음) |
| `400` | Bad Request | 요청 파라미터 유효성 오류 |
| `401` | Unauthorized | 인증 토큰 없음·만료·불일치 |
| `403` | Forbidden | 인증은 됐으나 권한 없음 |
| `404` | Not Found | 리소스 없음 |
| `409` | Conflict | 중복 데이터 (이메일 중복 등) |
| `429` | Too Many Requests | Rate Limit 초과 |
| `500` | Internal Server Error | 서버 내부 오류 |

### 1-4. 인증 방식

- **Access Token**: JWT RS256. 유효시간 1시간. `Authorization: Bearer {token}` 헤더 전달.
- **Refresh Token**: Opaque Token. Redis에 저장. 유효시간 14일. 요청 바디로 전달.
- **Gateway 처리**: Spring Cloud Gateway `JwtAuthenticationFilter`가 Access Token 검증 후 `X-User-Seq`, `X-User-Role` 헤더를 하위 서비스에 주입.

### 1-5. 날짜/시간 포맷

| 타입 | 포맷 | 예시 |
|---|---|---|
| DateTime | ISO 8601 UTC | `2026-04-13T09:00:00Z` |
| Date | ISO 8601 | `2000-06-22` |

---

## 2. 인증 API — /api/auth

> **서비스**: user-service (port 8081)
> **라우팅**: Spring Cloud Gateway → `lb://user-service`

---

### POST /api/auth/email/verify

**설명**: 이메일 인증 코드 발송 (회원가입 또는 비밀번호 재설정)
**인증 필요**: X

**Request Body**
```json
{
  "email": "chan@example.com",
  "type": "SIGNUP"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | String | Y | 인증 대상 이메일 |
| `type` | String | Y | `SIGNUP` \| `PWD_RESET` |

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": "인증 코드가 발송되었습니다. (5분 유효)"
}
```

**처리 흐름**
```
1. 이메일 형식 검증
2. type=SIGNUP 이면 이메일 중복 확인 → 중복 시 409
3. 6자리 랜덤 코드 생성
4. Redis SET email:verify:{TYPE}:{email} {code} EX 300
5. 이메일 발송 (비동기)
```

**에러 응답**

| 상황 | HTTP | code |
|---|---|---|
| 이메일 형식 오류 | 400 | `CMN_001` |
| 이메일 중복 (SIGNUP) | 409 | `USR_001` |

---

### POST /api/auth/email/verify/confirm

**설명**: 이메일 인증 코드 확인
**인증 필요**: X

**Request Body**
```json
{
  "email": "chan@example.com",
  "code": "391827",
  "type": "SIGNUP"
}
```

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": "이메일 인증이 완료되었습니다."
}
```

**처리 흐름**
```
1. Redis GET email:verify:{TYPE}:{email}
2. 코드 불일치 또는 키 미존재 → 401
3. 코드 일치 → DEL email:verify:{TYPE}:{email}
4. SET email:verified:{email} true EX 600
```

**에러 응답**

| 상황 | HTTP | code |
|---|---|---|
| 코드 불일치 또는 만료 | 401 | `USR_002` |

---

### POST /api/auth/signup

**설명**: 이메일 회원가입
**인증 필요**: X

**Request Body**
```json
{
  "email": "chan@example.com",
  "password": "Passw0rd!",
  "nickname": "채넬",
  "gender": "M",
  "birthDate": "1998-02-26"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|---|---|---|---|
| `email` | String | Y | 이메일 형식 |
| `password` | String | Y | 8~20자, 영문+숫자+특수문자 포함 |
| `nickname` | String | Y | 2~20자, 한글·영문·숫자 |
| `gender` | String | Y | `M` \| `F` \| `ETC` |
| `birthDate` | String | Y | `YYYY-MM-DD`, 만 18세 이상 |

**Response 201**
```json
{
  "success": true,
  "data": {
    "userSeq": 1001,
    "accessToken": "eyJhbGci...",
    "refreshToken": "abc123...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  },
  "message": "회원가입이 완료되었습니다."
}
```

**처리 흐름**
```
1. Redis GET email:verified:{email} → 미존재 시 401 (미인증)
2. 이메일 중복 최종 확인 → 중복 시 409
3. 비밀번호 BCrypt 해시 (strength 12)
4. tb_user INSERT
5. tb_user_profile INSERT (gender, birth_dt)
6. Redis DEL email:verified:{email}
7. JWT 발급 → Redis SET rt:{userSeq}:{deviceId} EX 1209600
```

**에러 응답**

| 상황 | HTTP | code |
|---|---|---|
| 이메일 미인증 | 401 | `USR_003` |
| 이메일 중복 | 409 | `USR_001` |
| 비밀번호 형식 오류 | 400 | `USR_004` |
| 미성년자 | 400 | `USR_005` |

---

### POST /api/auth/login

**설명**: 이메일 로그인
**인증 필요**: X

**Request Body**
```json
{
  "email": "chan@example.com",
  "password": "Passw0rd!",
  "deviceId": "web"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | String | Y | |
| `password` | String | Y | |
| `deviceId` | String | N | 멀티 디바이스 구분. 기본값: `default` |

**Response 200**
```json
{
  "success": true,
  "data": {
    "userSeq": 1001,
    "nickname": "채넬",
    "accessToken": "eyJhbGci...",
    "refreshToken": "abc123...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "profileCompleted": false
  }
}
```

**에러 응답**

| 상황 | HTTP | code |
|---|---|---|
| 이메일 미존재 또는 비밀번호 불일치 | 401 | `USR_006` |
| 정지 계정 | 403 | `USR_007` |
| 탈퇴 계정 | 401 | `USR_008` |

---

### POST /api/auth/logout

**설명**: 로그아웃 (Refresh Token 무효화)
**인증 필요**: O (Access Token)

**Request Body**
```json
{
  "refreshToken": "abc123...",
  "deviceId": "web"
}
```

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": "로그아웃되었습니다."
}
```

**처리 흐름**
```
Redis DEL rt:{userSeq}:{deviceId}
```

---

### POST /api/auth/refresh

**설명**: Access Token 재발급
**인증 필요**: X (Refresh Token)

**Request Body**
```json
{
  "refreshToken": "abc123...",
  "deviceId": "web"
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**에러 응답**

| 상황 | HTTP | code |
|---|---|---|
| Redis 키 미존재 (만료 또는 로그아웃) | 401 | `USR_009` |
| Refresh Token 불일치 | 401 | `USR_009` |

---

### POST /api/auth/oauth/{provider}

**설명**: 소셜 로그인 / 가입
**인증 필요**: X

**Path Variable**

| 변수 | 설명 |
|---|---|
| `provider` | `kakao` \| `naver` \| `google` |

**Request Body**
```json
{
  "authorizationCode": "abc...",
  "redirectUri": "https://seouldate.app/oauth/callback",
  "deviceId": "web"
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "userSeq": 1001,
    "nickname": "채넬",
    "accessToken": "eyJhbGci...",
    "refreshToken": "abc123...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "isNewUser": true,
    "profileCompleted": false
  }
}
```

| 필드 | 설명 |
|---|---|
| `isNewUser` | 신규 가입 여부 (true=가입, false=로그인) |
| `profileCompleted` | 프로필 완성 여부. false 시 온보딩 화면으로 유도 |

---

### PUT /api/auth/password

**설명**: 비밀번호 변경 (로그인 상태)
**인증 필요**: O

**Request Body**
```json
{
  "currentPassword": "OldPass1!",
  "newPassword": "NewPass2@"
}
```

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": "비밀번호가 변경되었습니다."
}
```

---

### POST /api/auth/password/reset

**설명**: 비밀번호 재설정 (비밀번호 분실)
**인증 필요**: X

**Request Body**
```json
{
  "email": "chan@example.com",
  "code": "391827",
  "newPassword": "NewPass2@"
}
```

**처리 흐름**
```
1. Redis GET email:verified:{email} → 미존재 시 401
2. 비밀번호 BCrypt 해시 후 UPDATE
3. Redis DEL email:verified:{email}
4. Redis DEL rt:{userSeq}:* (전체 디바이스 로그아웃)
```

---

### GET /api/auth/public-key

**설명**: JWT 검증용 RSA 공개키 제공 (Gateway 캐시 전용)
**인증 필요**: X

**Response 200**
```json
{
  "success": true,
  "data": {
    "publicKey": "MIIBIjANBgkq...",
    "algorithm": "RS256"
  }
}
```

---

## 3. 사용자 API — /api/users

> **서비스**: user-service (port 8081)
> **모든 API 인증 필요** (별도 표기 없으면 O)

---

### GET /api/users/me

**설명**: 내 전체 프로필 조회
**인증 필요**: O

**Response 200**
```json
{
  "success": true,
  "data": {
    "userSeq": 1001,
    "email": "chan@example.com",
    "nickNm": "채넬",
    "userRole": "USER",
    "profile": {
      "gndr": "M",
      "birthDt": "1998-02-26",
      "age": 28,
      "heightCm": 178,
      "bodyTypeCd": "NORMAL",
      "sidoNm": "서울특별시",
      "sggNm": "강남구",
      "jobNm": "개발자",
      "eduCd": "UNIVERSITY",
      "mbtiCd": "INTJ",
      "introCn": "안녕하세요!",
      "smokeCd": "NONE",
      "drinkCd": "SOMETIMES",
      "religionNm": null,
      "profileCmplYn": "Y"
    },
    "profileImages": [
      {
        "imgSeq": 1,
        "imgUrl": "https://minio.../users/1001/profile_0.webp",
        "sortOrd": 0
      }
    ],
    "interests": ["여행", "영화", "맛집"],
    "regDt": "2026-04-13T09:00:00Z"
  }
}
```

---

### PUT /api/users/me

**설명**: 기본 정보 수정 (닉네임)

**Request Body**
```json
{
  "nickNm": "뉴채넬"
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "nickNm": "뉴채넬"
  },
  "message": "기본 정보가 수정되었습니다."
}
```

---

### PUT /api/users/me/profile

**설명**: 상세 프로필 수정

**Request Body**
```json
{
  "heightCm": 178,
  "bodyTypeCd": "NORMAL",
  "sidoNm": "서울특별시",
  "sggNm": "강남구",
  "jobNm": "개발자",
  "eduCd": "UNIVERSITY",
  "mbtiCd": "INTJ",
  "introCn": "반갑습니다!",
  "smokeCd": "NONE",
  "drinkCd": "SOMETIMES",
  "religionNm": null
}
```

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": "프로필이 수정되었습니다."
}
```

**비고**: 필수 항목(heightCm, sidoNm, sggNm, introCn) 모두 입력 완료 시 `profile_cmpl_yn = 'Y'` 자동 갱신

---

### POST /api/users/me/images/presigned-url

**설명**: 프로필 이미지 업로드용 Presigned URL 발급

**Request Body**
```json
{
  "fileName": "profile.jpg",
  "contentType": "image/jpeg"
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "uploadUrl": "http://minio:9000/users/1001/profile_1.jpg?X-Amz-...",
    "objectKey": "users/1001/profile_1.jpg",
    "expiresIn": 300
  }
}
```

---

### POST /api/users/me/images

**설명**: 프로필 이미지 등록 (Presigned URL 업로드 완료 후 메타데이터 저장)

**Request Body**
```json
{
  "objectKey": "users/1001/profile_1.jpg",
  "sortOrd": 1
}
```

**Response 201**
```json
{
  "success": true,
  "data": {
    "imgSeq": 5,
    "imgUrl": "https://cdn.seouldate.app/users/1001/profile_1.webp",
    "sortOrd": 1
  }
}
```

**에러 응답**

| 상황 | HTTP | code |
|---|---|---|
| 이미지 6장 초과 | 400 | `USR_010` |

---

### PUT /api/users/me/images/{imgSeq}/main

**설명**: 대표 이미지 변경 (sort_ord = 0으로 설정)

**Path Variable**: `imgSeq` — 대표로 설정할 이미지 PK

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": "대표 이미지가 변경되었습니다."
}
```

---

### DELETE /api/users/me/images/{imgSeq}

**설명**: 프로필 이미지 삭제

**Response 204**: No Content

---

### POST /api/users/me/interests

**설명**: 관심사 저장 (전체 교체 방식)

**Request Body**
```json
{
  "interests": ["여행", "영화", "맛집", "운동"]
}
```

**검증**: 최대 10개, 각 항목 최대 20자

**Response 200**
```json
{
  "success": true,
  "data": {
    "interests": ["여행", "영화", "맛집", "운동"]
  },
  "message": "관심사가 저장되었습니다."
}
```

---

### GET /api/users/me/preferences

**설명**: 취향 설정 조회

**Response 200**
```json
{
  "success": true,
  "data": {
    "budgetLvlCd": "MEDIUM",
    "companionTpCd": "COUPLE",
    "moveTpCd": "TRANSIT",
    "styleTagVal": ["감성적", "조용한"],
    "foodCtgrVal": ["한식", "카페"],
    "prefAreaVal": ["강남", "홍대"]
  }
}
```

---

### PUT /api/users/me/preferences

**설명**: 취향 설정 저장/수정

**Request Body**
```json
{
  "budgetLvlCd": "MEDIUM",
  "companionTpCd": "COUPLE",
  "moveTpCd": "TRANSIT",
  "styleTagVal": ["감성적", "조용한"],
  "foodCtgrVal": ["한식", "카페"],
  "prefAreaVal": ["강남", "홍대"]
}
```

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": "취향 설정이 저장되었습니다."
}
```

---

### GET /api/users/me/bookmarks

**설명**: 나의 북마크 목록 조회

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `tgtTpCd` | String | N | 전체 | `PLACE` \| `COURSE` \| `EVENT` |
| `page` | int | N | `0` | 페이지 번호 |
| `size` | int | N | `20` | 페이지 크기 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "bookmarkSeq": 1,
        "tgtTpCd": "PLACE",
        "tgtSeq": 200,
        "tgtNm": "카페 드롭탑 강남점",
        "thumbImgUrl": "https://cdn.seouldate.app/places/200/thumb.webp",
        "regDt": "2026-04-13T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "hasNext": false
  }
}
```

---

### POST /api/users/me/bookmarks

**설명**: 북마크 추가

**Request Body**
```json
{
  "tgtTpCd": "PLACE",
  "tgtSeq": 200,
  "tgtNm": "카페 드롭탑 강남점",
  "thumbImgUrl": "https://cdn.seouldate.app/places/200/thumb.webp"
}
```

**Response 201**
```json
{
  "success": true,
  "data": {
    "bookmarkSeq": 1
  },
  "message": "북마크에 추가되었습니다."
}
```

**에러 응답**

| 상황 | HTTP | code |
|---|---|---|
| 이미 북마크된 항목 | 409 | `USR_011` |

---

### DELETE /api/users/me/bookmarks/{bookmarkSeq}

**설명**: 북마크 삭제

**Response 204**: No Content

---

### DELETE /api/users/me

**설명**: 회원 탈퇴 (Soft Delete)

**Request Body**
```json
{
  "password": "Passw0rd!",
  "reason": "서비스 미사용"
}
```

**처리 흐름**
```
1. 비밀번호 검증 (소셜 전용 계정은 생략)
2. tb_user: del_yn='Y', del_dt=NOW(), user_stt='DELETED'
3. Redis DEL rt:{userSeq}:* (전체 디바이스 로그아웃)
4. [배치] 30일 후 하드 DELETE
```

**Response 204**: No Content

---

### GET /api/users/{userSeq}

**설명**: 특정 사용자 프로필 조회 (공개 정보만)
**인증 필요**: O

**Response 200**
```json
{
  "success": true,
  "data": {
    "userSeq": 1002,
    "nickNm": "서울러",
    "profile": {
      "gndr": "F",
      "age": 25,
      "heightCm": 163,
      "bodyTypeCd": "SLIM",
      "sidoNm": "서울특별시",
      "sggNm": "마포구",
      "jobNm": "디자이너",
      "mbtiCd": "ENFP",
      "introCn": "안녕하세요!",
      "smokeCd": "NONE",
      "drinkCd": "SOMETIMES"
    },
    "profileImages": [
      {
        "imgUrl": "https://cdn.seouldate.app/users/1002/profile_0.webp",
        "sortOrd": 0
      }
    ],
    "interests": ["여행", "카페"]
  }
}
```

**비고**: 차단된 사용자 조회 시 404 반환 (존재 노출 방지)

---

### POST /api/users/{userSeq}/block

**설명**: 사용자 차단

**Response 201**
```json
{
  "success": true,
  "data": null,
  "message": "차단되었습니다."
}
```

**에러 응답**

| 상황 | HTTP | code |
|---|---|---|
| 자기 자신 차단 | 400 | `USR_012` |
| 이미 차단된 사용자 | 409 | `USR_013` |

---

### DELETE /api/users/{userSeq}/block

**설명**: 차단 해제

**Response 204**: No Content

---

### POST /api/users/{userSeq}/report

**설명**: 사용자 신고

**Request Body**
```json
{
  "reportRsn": "FAKE_PROFILE",
  "reportCn": "프로필 사진이 연예인 사진을 도용한 것 같습니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `reportRsn` | String | Y | `SPAM` \| `FAKE_PROFILE` \| `ABUSE` \| `INAPPROPRIATE` \| `OTHER` |
| `reportCn` | String | N | 상세 내용 (최대 500자) |

**Response 201**
```json
{
  "success": true,
  "data": null,
  "message": "신고가 접수되었습니다."
}
```

---

## 4. 내부 통신 API — /api/internal

> **서비스**: user-service (port 8081)
> **접근 제한**: Spring Cloud Gateway 통과 없이 서비스 내부 직접 호출
> **인증**: Gateway에서 주입한 `X-Internal-Service` 헤더 검증
> **외부 노출**: X (Gateway 라우팅 제외)

---

### GET /api/internal/users/{userSeq}

**설명**: userId로 사용자 기본 정보 조회
**호출 주체**: recommendation-service, place-service 등

**Response 200**
```json
{
  "success": true,
  "data": {
    "userSeq": 1001,
    "nickNm": "채넬",
    "gndr": "M",
    "age": 28,
    "sidoNm": "서울특별시",
    "sggNm": "강남구",
    "profileCmplYn": "Y",
    "userStt": "ACTIVE"
  }
}
```

---

### GET /api/internal/users/{userSeq}/exists

**설명**: 사용자 존재·활성 여부 확인
**호출 주체**: 타 서비스 (데이터 정합성 확인용)

**Response 200**
```json
{
  "success": true,
  "data": {
    "exists": true,
    "active": true
  }
}
```

---

### GET /api/internal/users/{userSeq}/preferences

**설명**: 사용자 취향 설정 조회 (추천 서비스 입력값)
**호출 주체**: recommendation-service

**Response 200**
```json
{
  "success": true,
  "data": {
    "budgetLvlCd": "MEDIUM",
    "companionTpCd": "COUPLE",
    "moveTpCd": "TRANSIT",
    "styleTagVal": ["감성적", "조용한"],
    "foodCtgrVal": ["한식", "카페"],
    "prefAreaVal": ["강남", "홍대"],
    "interests": ["여행", "영화", "맛집"]
  }
}
```

---

## 5. 에러 코드 정의

### 5-1. 공통 에러

| code | HTTP | 설명 |
|---|---|---|
| `CMN_001` | 400 | 요청 파라미터 유효성 오류 |
| `CMN_002` | 401 | Access Token 없음 또는 만료 |
| `CMN_003` | 403 | 접근 권한 없음 |
| `CMN_004` | 404 | 리소스 없음 |
| `CMN_005` | 429 | Rate Limit 초과 |
| `CMN_999` | 500 | 서버 내부 오류 |

### 5-2. 사용자 도메인 에러

| code | HTTP | 설명 |
|---|---|---|
| `USR_001` | 409 | 이메일 중복 |
| `USR_002` | 401 | 이메일 인증 코드 불일치 또는 만료 |
| `USR_003` | 401 | 이메일 인증 미완료 |
| `USR_004` | 400 | 비밀번호 형식 오류 |
| `USR_005` | 400 | 가입 제한 연령 (만 18세 미만) |
| `USR_006` | 401 | 이메일 또는 비밀번호 불일치 |
| `USR_007` | 403 | 정지 계정 |
| `USR_008` | 401 | 탈퇴 계정 |
| `USR_009` | 401 | Refresh Token 만료 또는 불일치 |
| `USR_010` | 400 | 프로필 이미지 최대 6장 초과 |
| `USR_011` | 409 | 이미 북마크된 항목 |
| `USR_012` | 400 | 자기 자신 차단 불가 |
| `USR_013` | 409 | 이미 차단된 사용자 |

---

## 6. 보안 정책

### 6-1. Rate Limit (Gateway 적용)

| 대상 | 제한 | 기준 |
|---|---|---|
| 전체 API | 100 req/min | IP 기준 (Redis Token Bucket) |
| `/api/auth/email/verify` | 5 req/10min | IP + 이메일 기준 (스팸 방지) |
| `/api/auth/login` | 10 req/min | IP 기준 (브루트포스 방지) |
| AI 추천 API | 10 req/hour | 사용자 기준 (LLM 비용 통제) |

### 6-2. 민감 정보 처리

| 항목 | 처리 방식 |
|---|---|
| 비밀번호 | BCrypt 해시 (strength 12). 원문 저장 금지 |
| Refresh Token | SHA-256 해시 후 Redis 저장. 원문은 클라이언트만 보유 |
| 소셜 Access Token | 저장하지 않음. 사용자 정보 조회 후 즉시 폐기 |
| 응답 마스킹 | 이메일 앞 3자리만 노출 (예: `cha***@example.com`) |

### 6-3. Gateway 헤더 주입 (하위 서비스 신뢰)

```
# Gateway → 하위 서비스 주입 헤더 (JWT 검증 후)
X-User-Seq: 1001
X-User-Role: USER

# 내부 서비스 간 통신
X-Internal-Service: recommendation-service
```

하위 서비스는 `X-User-Seq` 헤더를 신뢰하여 별도 JWT 검증 없이 사용자 식별. `Authorization` 헤더는 Gateway에서 소비하고 하위로 전달하지 않음.
