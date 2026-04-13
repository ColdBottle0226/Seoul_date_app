package com.seouldate.user.controller;

import com.seouldate.user.service.BookmarkService;
import com.seouldate.user.service.UserBlockService;
import com.seouldate.user.service.UserReportService;
import com.seouldate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 프로필/설정 컨트롤러 (/api/users)
 *
 * <p>인증 필요 API: Gateway 가 X-User-Seq 헤더를 주입한다.
 * 컨트롤러는 헤더를 {@code @RequestHeader("X-User-Seq")} 로 수신한다.
 *
 * <p>각 엔드포인트는 Issue 별 작업으로 구현 예정.
 *
 * <pre>
 * GET    /api/users/me                         내 프로필 조회
 * PUT    /api/users/me                         기본 정보 수정
 * PUT    /api/users/me/profile                 상세 프로필 수정
 * POST   /api/users/me/images/presigned-url    Presigned URL 발급
 * POST   /api/users/me/images                  이미지 등록
 * PUT    /api/users/me/images/{imgSeq}/main    대표 이미지 변경
 * DELETE /api/users/me/images/{imgSeq}         이미지 삭제
 * POST   /api/users/me/interests               관심사 저장
 * GET    /api/users/me/preferences             취향 설정 조회
 * PUT    /api/users/me/preferences             취향 설정 저장
 * GET    /api/users/me/bookmarks               북마크 목록
 * POST   /api/users/me/bookmarks               북마크 추가
 * DELETE /api/users/me/bookmarks/{bookmarkSeq} 북마크 삭제
 * DELETE /api/users/me                         회원 탈퇴
 * GET    /api/users/{userSeq}                  타인 프로필 조회
 * POST   /api/users/{userSeq}/block            차단
 * DELETE /api/users/{userSeq}/block            차단 해제
 * POST   /api/users/{userSeq}/report           신고
 * </pre>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final BookmarkService bookmarkService;
    private final UserBlockService userBlockService;
    private final UserReportService userReportService;

    // TODO: 각 엔드포인트 구현 (Issue 별 PR)




}
