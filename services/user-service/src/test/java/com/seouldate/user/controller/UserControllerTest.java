package com.seouldate.user.controller;

import com.seouldate.user.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 슬라이스 테스트.
 *
 * <p>테스트 범위: HTTP 요청/응답, 입력값 유효성 검증, 에러 코드 매핑
 *
 * <p>인증은 Gateway가 처리하고, 하위 서비스는 X-User-Seq / X-User-Role 헤더를 신뢰한다.
 * 따라서 인증 필요 API 테스트는 {@code mockUserHeader()} 헬퍼로 헤더를 수동 주입한다.
 *
 * <p>설계 문서 참고:
 * <ul>
 *   <li>API 설계서 §3 — 사용자 API /api/users</li>
 *   <li>테이블 설계서 §4-3 ~ §4-9</li>
 * </ul>
 */
// TODO: @MockBean UserService userService;
// TODO: @MockBean BookmarkService bookmarkService;
// TODO: @MockBean UserBlockService userBlockService;
// TODO: @MockBean UserReportService userReportService;
@WebMvcTest // TODO: @WebMvcTest(UserController.class) 로 변경
class UserControllerTest extends ControllerTestSupport {

    // =========================================================================
    // GET /api/users/me — 내 전체 프로필 조회
    // =========================================================================

    @Nested
    @DisplayName("GET /api/users/me — 내 전체 프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("성공: 프로필 완성 사용자 조회 → 200 + 전체 프로필 반환")
        void success_profile_completed() throws Exception {
            // given
            // TODO: X-User-Seq: 1001, X-User-Role: USER 헤더 설정
            // TODO: userService.getMyProfile(1001L) 반환값 stubbing (프로필 완성 상태)

            // when & then
            // TODO: GET /api/users/me 요청
            // TODO: status().isOk() 검증
            // TODO: jsonPath("$.data.userSeq").value(1001) 검증
            // TODO: jsonPath("$.data.profile").exists() 검증
            // TODO: jsonPath("$.data.profileImages").isArray() 검증
            // TODO: jsonPath("$.data.interests").isArray() 검증
        }

        @Test
        @DisplayName("성공: 프로필 미완성 사용자 조회 → 200 + profileCmplYn=N")
        void success_profile_incomplete() throws Exception {
            // given
            // TODO: userService.getMyProfile() 반환값 stubbing (profileCmplYn=N)

            // when & then
            // TODO: jsonPath("$.data.profile.profileCmplYn").value("N") 검증
        }

        @Test
        @DisplayName("실패: X-User-Seq 헤더 없음 → 401")
        void fail_no_auth_header() throws Exception {
            // given
            // TODO: 헤더 없이 요청

            // when & then
            // TODO: status().isUnauthorized() 검증
        }
    }

    // =========================================================================
    // PUT /api/users/me — 기본 정보 수정 (닉네임)
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/users/me — 기본 정보 수정")
    class UpdateMyBasicInfo {

        @Test
        @DisplayName("성공: 닉네임 변경 → 200 + 변경된 닉네임 반환")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 요청 바디 { nickNm: "뉴채넬" }
            // TODO: userService.updateBasicInfo() 정상 동작 stubbing

            // when & then
            // TODO: status().isOk() 검증
            // TODO: jsonPath("$.data.nickNm").value("뉴채넬") 검증
        }

        @Test
        @DisplayName("실패: 닉네임 2자 미만 → 400 CMN_001")
        void fail_nickname_too_short() throws Exception {
            // given
            // TODO: 요청 바디 { nickNm: "A" }

            // when & then
            // TODO: status().isBadRequest() 검증
        }

        @Test
        @DisplayName("실패: 닉네임 20자 초과 → 400 CMN_001")
        void fail_nickname_too_long() throws Exception {
            // given
            // TODO: 요청 바디 { nickNm: "21자짜리닉네임문자열12345678901" }

            // when & then
            // TODO: status().isBadRequest() 검증
        }
    }

    // =========================================================================
    // PUT /api/users/me/profile — 상세 프로필 수정
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/users/me/profile — 상세 프로필 수정")
    class UpdateMyDetailProfile {

        @Test
        @DisplayName("성공: 전체 필드 입력 → 200 + profileCmplYn=Y 자동 갱신")
        void success_all_required_fields_filled() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 요청 바디 { heightCm, bodyTypeCd, sidoNm, sggNm, jobNm, eduCd, mbtiCd, introCn, smokeCd, drinkCd }
            // TODO: userService.updateDetailProfile() → profileCmplYn="Y" 반환 stubbing

            // when & then
            // TODO: status().isOk() 검증
        }

        @Test
        @DisplayName("성공: 필수 항목(heightCm, sidoNm, sggNm, introCn) 중 일부 미입력 → 200 + profileCmplYn=N")
        void success_required_fields_incomplete() throws Exception {
            // given
            // TODO: 요청 바디에서 sidoNm 제외
            // TODO: userService.updateDetailProfile() → profileCmplYn="N" 반환 stubbing

            // when & then
            // TODO: status().isOk() 검증
        }

        @Test
        @DisplayName("실패: introCn 500자 초과 → 400 CMN_001")
        void fail_intro_too_long() throws Exception {
            // given
            // TODO: 요청 바디 { introCn: 501자 문자열 }

            // when & then
            // TODO: status().isBadRequest() 검증
        }

        @Test
        @DisplayName("실패: bodyTypeCd 허용되지 않는 값 → 400 CMN_001")
        void fail_invalid_body_type_code() throws Exception {
            // given
            // TODO: 요청 바디 { bodyTypeCd: "UNKNOWN" }

            // when & then
            // TODO: status().isBadRequest() 검증
        }
    }

    // =========================================================================
    // POST /api/users/me/images/presigned-url — Presigned URL 발급
    // =========================================================================

    @Nested
    @DisplayName("POST /api/users/me/images/presigned-url — Presigned URL 발급")
    class GetPresignedUrl {

        @Test
        @DisplayName("성공: 유효한 이미지 파일 정보 → 200 + uploadUrl 반환")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 요청 바디 { fileName: "profile.jpg", contentType: "image/jpeg" }
            // TODO: userService.generatePresignedUrl() 반환값 stubbing

            // when & then
            // TODO: status().isOk() 검증
            // TODO: jsonPath("$.data.uploadUrl").exists() 검증
            // TODO: jsonPath("$.data.objectKey").exists() 검증
            // TODO: jsonPath("$.data.expiresIn").value(300) 검증
        }

        @Test
        @DisplayName("실패: 지원하지 않는 contentType → 400")
        void fail_unsupported_content_type() throws Exception {
            // given
            // TODO: 요청 바디 { fileName: "doc.pdf", contentType: "application/pdf" }

            // when & then
            // TODO: status().isBadRequest() 검증
        }
    }

    // =========================================================================
    // POST /api/users/me/images — 프로필 이미지 등록
    // =========================================================================

    @Nested
    @DisplayName("POST /api/users/me/images — 프로필 이미지 등록")
    class RegisterProfileImage {

        @Test
        @DisplayName("성공: 이미지 6장 미만일 때 등록 → 201 + imgSeq 반환")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 요청 바디 { objectKey: "users/1001/profile_1.jpg", sortOrd: 1 }
            // TODO: userService.registerProfileImage() 반환값 stubbing

            // when & then
            // TODO: status().isCreated() 검증
            // TODO: jsonPath("$.data.imgSeq").isNumber() 검증
            // TODO: jsonPath("$.data.imgUrl").exists() 검증
        }

        @Test
        @DisplayName("실패: 이미지 6장 초과 시 등록 → 400 USR_010")
        void fail_image_limit_exceeded() throws Exception {
            // given
            // TODO: userService.registerProfileImage() → ProfileImageLimitException 발생 stubbing

            // when & then
            // TODO: status().isBadRequest() 검증
            // TODO: jsonPath("$.code").value("USR_010") 검증
        }
    }

    // =========================================================================
    // PUT /api/users/me/images/{imgSeq}/main — 대표 이미지 변경
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/users/me/images/{imgSeq}/main — 대표 이미지 변경")
    class SetMainProfileImage {

        @Test
        @DisplayName("성공: 내 이미지를 대표로 변경 → 200")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: path variable imgSeq = 5
            // TODO: userService.setMainImage() 정상 동작 stubbing

            // when & then
            // TODO: status().isOk() 검증
        }

        @Test
        @DisplayName("실패: 다른 사용자의 이미지 변경 시도 → 403 CMN_003")
        void fail_not_owner() throws Exception {
            // given
            // TODO: userService.setMainImage() → AccessDeniedException 발생 stubbing

            // when & then
            // TODO: status().isForbidden() 검증
        }

        @Test
        @DisplayName("실패: 존재하지 않는 imgSeq → 404 CMN_004")
        void fail_image_not_found() throws Exception {
            // given
            // TODO: userService.setMainImage() → ResourceNotFoundException 발생 stubbing

            // when & then
            // TODO: status().isNotFound() 검증
        }
    }

    // =========================================================================
    // DELETE /api/users/me/images/{imgSeq} — 프로필 이미지 삭제
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/users/me/images/{imgSeq} — 프로필 이미지 삭제")
    class DeleteProfileImage {

        @Test
        @DisplayName("성공: 내 이미지 삭제 → 204 No Content")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: path variable imgSeq = 5
            // TODO: userService.deleteProfileImage() 정상 동작 stubbing

            // when & then
            // TODO: DELETE /api/users/me/images/5 요청
            // TODO: status().isNoContent() 검증
        }

        @Test
        @DisplayName("실패: 다른 사용자의 이미지 삭제 시도 → 403 CMN_003")
        void fail_not_owner() throws Exception {
            // given
            // TODO: userService.deleteProfileImage() → AccessDeniedException 발생 stubbing

            // when & then
            // TODO: status().isForbidden() 검증
        }
    }

    // =========================================================================
    // POST /api/users/me/interests — 관심사 저장 (전체 교체)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/users/me/interests — 관심사 저장")
    class SaveInterests {

        @Test
        @DisplayName("성공: 관심사 10개 이하 입력 → 200 + 저장된 관심사 반환")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 요청 바디 { interests: ["여행", "영화", "맛집", "운동"] }
            // TODO: userService.saveInterests() 반환값 stubbing

            // when & then
            // TODO: status().isOk() 검증
            // TODO: jsonPath("$.data.interests").isArray() 검증
            // TODO: jsonPath("$.data.interests.length()").value(4) 검증
        }

        @Test
        @DisplayName("실패: 관심사 10개 초과 → 400 CMN_001")
        void fail_interests_limit_exceeded() throws Exception {
            // given
            // TODO: 요청 바디 { interests: 11개 항목 배열 }

            // when & then
            // TODO: status().isBadRequest() 검증
        }

        @Test
        @DisplayName("실패: 관심사 항목이 20자 초과 → 400 CMN_001")
        void fail_interest_item_too_long() throws Exception {
            // given
            // TODO: 요청 바디 { interests: ["21자짜리관심사항목입니다123456789"] }

            // when & then
            // TODO: status().isBadRequest() 검증
        }
    }

    // =========================================================================
    // GET /api/users/me/preferences — 취향 설정 조회
    // =========================================================================

    @Nested
    @DisplayName("GET /api/users/me/preferences — 취향 설정 조회")
    class GetMyPreferences {

        @Test
        @DisplayName("성공: 취향 설정 존재 → 200 + 설정값 반환")
        void success_preferences_exist() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: userService.getPreferences(1001L) 반환값 stubbing

            // when & then
            // TODO: status().isOk() 검증
            // TODO: jsonPath("$.data.budgetLvlCd").exists() 검증
            // TODO: jsonPath("$.data.styleTagVal").isArray() 검증
        }

        @Test
        @DisplayName("성공: 취향 설정 미입력 상태 → 200 + 기본값 반환")
        void success_no_preferences_yet() throws Exception {
            // given
            // TODO: userService.getPreferences() 기본값 stubbing

            // when & then
            // TODO: status().isOk() 검증
        }
    }

    // =========================================================================
    // PUT /api/users/me/preferences — 취향 설정 저장/수정
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/users/me/preferences — 취향 설정 저장/수정")
    class UpdateMyPreferences {

        @Test
        @DisplayName("성공: 취향 설정 저장 → 200")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 요청 바디 { budgetLvlCd: "MEDIUM", companionTpCd: "COUPLE", moveTpCd: "TRANSIT", styleTagVal: [...], ... }
            // TODO: userService.savePreferences() 정상 동작 stubbing

            // when & then
            // TODO: status().isOk() 검증
        }

        @Test
        @DisplayName("실패: budgetLvlCd 허용되지 않는 값 → 400 CMN_001")
        void fail_invalid_budget_level() throws Exception {
            // given
            // TODO: 요청 바디 { budgetLvlCd: "EXTREME" }

            // when & then
            // TODO: status().isBadRequest() 검증
        }
    }

    // =========================================================================
    // GET /api/users/me/bookmarks — 북마크 목록 조회
    // =========================================================================

    @Nested
    @DisplayName("GET /api/users/me/bookmarks — 북마크 목록 조회")
    class GetMyBookmarks {

        @Test
        @DisplayName("성공: 전체 타입 북마크 조회 → 200 + 페이지네이션 응답")
        void success_all_types() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 쿼리 파라미터 page=0, size=20
            // TODO: bookmarkService.getBookmarks() 반환값 stubbing

            // when & then
            // TODO: GET /api/users/me/bookmarks?page=0&size=20 요청
            // TODO: status().isOk() 검증
            // TODO: jsonPath("$.data.content").isArray() 검증
            // TODO: jsonPath("$.data.hasNext").isBoolean() 검증
        }

        @Test
        @DisplayName("성공: PLACE 타입만 필터링 조회 → 200")
        void success_filter_by_type() throws Exception {
            // given
            // TODO: 쿼리 파라미터 tgtTpCd=PLACE
            // TODO: bookmarkService.getBookmarks(tgtTpCd=PLACE) 반환값 stubbing

            // when & then
            // TODO: status().isOk() 검증
        }

        @Test
        @DisplayName("실패: tgtTpCd 허용되지 않는 값 → 400")
        void fail_invalid_target_type() throws Exception {
            // given
            // TODO: 쿼리 파라미터 tgtTpCd=INVALID

            // when & then
            // TODO: status().isBadRequest() 검증
        }
    }

    // =========================================================================
    // POST /api/users/me/bookmarks — 북마크 추가
    // =========================================================================

    @Nested
    @DisplayName("POST /api/users/me/bookmarks — 북마크 추가")
    class AddBookmark {

        @Test
        @DisplayName("성공: 신규 북마크 추가 → 201 + bookmarkSeq 반환")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 요청 바디 { tgtTpCd: "PLACE", tgtSeq: 200, tgtNm: "카페 드롭탑", thumbImgUrl: "..." }
            // TODO: bookmarkService.addBookmark() 반환값 stubbing

            // when & then
            // TODO: status().isCreated() 검증
            // TODO: jsonPath("$.data.bookmarkSeq").isNumber() 검증
        }

        @Test
        @DisplayName("실패: 이미 북마크된 항목 → 409 USR_011")
        void fail_already_bookmarked() throws Exception {
            // given
            // TODO: bookmarkService.addBookmark() → DuplicateBookmarkException 발생 stubbing

            // when & then
            // TODO: status().isConflict() 검증
            // TODO: jsonPath("$.code").value("USR_011") 검증
        }
    }

    // =========================================================================
    // DELETE /api/users/me/bookmarks/{bookmarkSeq} — 북마크 삭제
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/users/me/bookmarks/{bookmarkSeq} — 북마크 삭제")
    class DeleteBookmark {

        @Test
        @DisplayName("성공: 내 북마크 삭제 → 204 No Content")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: path variable bookmarkSeq = 1
            // TODO: bookmarkService.deleteBookmark() 정상 동작 stubbing

            // when & then
            // TODO: DELETE /api/users/me/bookmarks/1 요청
            // TODO: status().isNoContent() 검증
        }

        @Test
        @DisplayName("실패: 존재하지 않는 북마크 → 404 CMN_004")
        void fail_bookmark_not_found() throws Exception {
            // given
            // TODO: bookmarkService.deleteBookmark() → ResourceNotFoundException 발생 stubbing

            // when & then
            // TODO: status().isNotFound() 검증
        }
    }

    // =========================================================================
    // DELETE /api/users/me — 회원 탈퇴 (Soft Delete)
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/users/me — 회원 탈퇴")
    class WithdrawUser {

        @Test
        @DisplayName("성공: 이메일 계정 — 비밀번호 검증 후 탈퇴 → 204 No Content")
        void success_email_account() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정
            // TODO: 요청 바디 { password: "Passw0rd!", reason: "서비스 미사용" }
            // TODO: userService.withdraw() 정상 동작 stubbing (del_yn=Y, Redis rt 삭제)

            // when & then
            // TODO: DELETE /api/users/me 요청
            // TODO: status().isNoContent() 검증
        }

        @Test
        @DisplayName("성공: 소셜 전용 계정 — 비밀번호 없이 탈퇴 → 204 No Content")
        void success_social_account() throws Exception {
            // given
            // TODO: 소셜 계정 사용자 X-User-Seq 헤더 설정
            // TODO: 요청 바디 { reason: "다른 서비스 이용" } — password 생략

            // when & then
            // TODO: status().isNoContent() 검증
        }

        @Test
        @DisplayName("실패: 이메일 계정에서 비밀번호 불일치 → 401 USR_006")
        void fail_wrong_password() throws Exception {
            // given
            // TODO: userService.withdraw() → InvalidCredentialsException 발생 stubbing

            // when & then
            // TODO: status().isUnauthorized() 검증
        }
    }

    // =========================================================================
    // GET /api/users/{userSeq} — 특정 사용자 프로필 조회
    // =========================================================================

    @Nested
    @DisplayName("GET /api/users/{userSeq} — 특정 사용자 프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("성공: 공개 프로필 조회 → 200 + 공개 정보만 반환")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq 헤더 설정 (조회하는 사용자)
            // TODO: path variable userSeq = 1002
            // TODO: userService.getUserProfile(requestUserSeq=1001, targetUserSeq=1002) 반환값 stubbing

            // when & then
            // TODO: status().isOk() 검증
            // TODO: jsonPath("$.data.userSeq").value(1002) 검증
            // TODO: jsonPath("$.data.profile.gndr").exists() 검증
            // TODO: 민감 정보(email, passwd) 응답에 미포함 검증
        }

        @Test
        @DisplayName("실패: 차단된 사용자 조회 → 404 CMN_004 (존재 노출 방지)")
        void fail_blocked_user() throws Exception {
            // given
            // TODO: userService.getUserProfile() → BlockedUserException 발생 → 404 반환 stubbing

            // when & then
            // TODO: status().isNotFound() 검증
        }

        @Test
        @DisplayName("실패: 탈퇴한 사용자 조회 → 404 CMN_004")
        void fail_deleted_user() throws Exception {
            // given
            // TODO: userService.getUserProfile() → ResourceNotFoundException 발생 stubbing

            // when & then
            // TODO: status().isNotFound() 검증
        }
    }

    // =========================================================================
    // POST /api/users/{userSeq}/block — 사용자 차단
    // =========================================================================

    @Nested
    @DisplayName("POST /api/users/{userSeq}/block — 사용자 차단")
    class BlockUser {

        @Test
        @DisplayName("성공: 다른 사용자 차단 → 201")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq: 1001 헤더 설정
            // TODO: path variable userSeq = 1002
            // TODO: userBlockService.block(blocker=1001, blocked=1002) 정상 동작 stubbing

            // when & then
            // TODO: status().isCreated() 검증
        }

        @Test
        @DisplayName("실패: 자기 자신 차단 → 400 USR_012")
        void fail_self_block() throws Exception {
            // given
            // TODO: X-User-Seq: 1001, path variable userSeq = 1001 (동일)
            // TODO: userBlockService.block() → SelfBlockException 발생 stubbing

            // when & then
            // TODO: status().isBadRequest() 검증
            // TODO: jsonPath("$.code").value("USR_012") 검증
        }

        @Test
        @DisplayName("실패: 이미 차단된 사용자 → 409 USR_013")
        void fail_already_blocked() throws Exception {
            // given
            // TODO: userBlockService.block() → AlreadyBlockedException 발생 stubbing

            // when & then
            // TODO: status().isConflict() 검증
            // TODO: jsonPath("$.code").value("USR_013") 검증
        }
    }

    // =========================================================================
    // DELETE /api/users/{userSeq}/block — 차단 해제
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/users/{userSeq}/block — 차단 해제")
    class UnblockUser {

        @Test
        @DisplayName("성공: 차단 해제 → 204 No Content")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq: 1001 헤더 설정
            // TODO: path variable userSeq = 1002
            // TODO: userBlockService.unblock() 정상 동작 stubbing

            // when & then
            // TODO: status().isNoContent() 검증
        }

        @Test
        @DisplayName("실패: 차단하지 않은 사용자 차단 해제 시도 → 404")
        void fail_not_blocked() throws Exception {
            // given
            // TODO: userBlockService.unblock() → ResourceNotFoundException 발생 stubbing

            // when & then
            // TODO: status().isNotFound() 검증
        }
    }

    // =========================================================================
    // POST /api/users/{userSeq}/report — 사용자 신고
    // =========================================================================

    @Nested
    @DisplayName("POST /api/users/{userSeq}/report — 사용자 신고")
    class ReportUser {

        @Test
        @DisplayName("성공: FAKE_PROFILE 사유로 신고 → 201")
        void success() throws Exception {
            // given
            // TODO: X-User-Seq: 1001 헤더 설정
            // TODO: path variable userSeq = 1002
            // TODO: 요청 바디 { reportRsn: "FAKE_PROFILE", reportCn: "설명..." }
            // TODO: userReportService.report() 정상 동작 stubbing

            // when & then
            // TODO: status().isCreated() 검증
        }

        @Test
        @DisplayName("성공: reportCn 없이 신고 (선택 필드) → 201")
        void success_without_content() throws Exception {
            // given
            // TODO: 요청 바디 { reportRsn: "SPAM" } — reportCn 생략

            // when & then
            // TODO: status().isCreated() 검증
        }

        @Test
        @DisplayName("실패: reportRsn 허용되지 않는 값 → 400 CMN_001")
        void fail_invalid_report_reason() throws Exception {
            // given
            // TODO: 요청 바디 { reportRsn: "INVALID_REASON" }

            // when & then
            // TODO: status().isBadRequest() 검증
        }

        @Test
        @DisplayName("실패: reportCn 500자 초과 → 400 CMN_001")
        void fail_report_content_too_long() throws Exception {
            // given
            // TODO: 요청 바디 { reportRsn: "SPAM", reportCn: 501자 문자열 }

            // when & then
            // TODO: status().isBadRequest() 검증
        }
    }
}
