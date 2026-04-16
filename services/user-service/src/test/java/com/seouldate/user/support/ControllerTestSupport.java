package com.seouldate.user.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seouldate.user.controller.AuthController;
import com.seouldate.user.controller.UserController;
import com.seouldate.user.service.AuthService;
import com.seouldate.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 테스트 공통 기반 클래스
 *
 * ─────────────────────────────────────────────────────────────────────────
 * @WebMvcTest 란?
 * ─────────────────────────────────────────────────────────────────────────
 * Spring MVC 의 웹 레이어(컨트롤러, 필터, 유효성 검사)만 로드하는 슬라이스 테스트입니다.
 * DB 나 실제 서비스는 로드하지 않아 빠르게 실행됩니다.
 *
 * 반면 @SpringBootTest 는 전체 애플리케이션 컨텍스트를 로드하므로
 * 무겁고 느립니다. 컨트롤러 테스트에는 @WebMvcTest 가 적합합니다.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * @MockBean 이란?
 * ─────────────────────────────────────────────────────────────────────────
 * 실제 Service 를 대신하는 가짜(Mock) 객체를 Spring 컨텍스트에 등록합니다.
 * 테스트에서 이 Mock 의 동작을 직접 지정(stubbing)할 수 있습니다.
 *
 * @Mock vs @MockBean 차이:
 * - @Mock     : Mockito 가 생성. Spring 컨텍스트와 무관 (Service 테스트용)
 * - @MockBean : Mockito + Spring 컨텍스트에 등록 (Controller 테스트용)
 *
 * ─────────────────────────────────────────────────────────────────────────
 * MockMvc 란?
 * ─────────────────────────────────────────────────────────────────────────
 * 실제 HTTP 서버 없이 컨트롤러를 테스트하는 도구입니다.
 * perform() → andExpect() 체이닝으로 요청/응답을 검증합니다.
 *
 * 기본 사용 패턴:
 * <pre>
 *     mockMvc.perform(
 *             post("/api/auth/signup")
 *                 .contentType(MediaType.APPLICATION_JSON)
 *                 .content(objectMapper.writeValueAsString(request))
 *         )
 *         .andExpect(status().isCreated())                          // HTTP 상태 코드
 *         .andExpect(jsonPath("$.success").value(true))            // JSON 필드 검증
 *         .andExpect(jsonPath("$.data.userSeq").isNumber());       // 숫자 타입 검증
 * </pre>
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 인증 헤더 추가 방법 (X-User-Seq)
 * ─────────────────────────────────────────────────────────────────────────
 * 이 프로젝트는 GatewayAuthFilter 가 X-User-Seq 헤더를 읽어 인증을 처리합니다.
 * 컨트롤러 테스트에서 인증된 요청을 보내려면 헤더를 직접 추가하세요:
 * <pre>
 *     mockMvc.perform(
 *             get("/api/users/1")
 *                 .header("X-User-Seq", "1")
 *                 .header("X-User-Role", "USER")
 *         )
 *         .andExpect(status().isOk());
 * </pre>
 *
 * ─────────────────────────────────────────────────────────────────────────
 * ObjectMapper 란?
 * ─────────────────────────────────────────────────────────────────────────
 * Java 객체 ↔ JSON 문자열 변환 도구입니다.
 * - 직렬화: objectMapper.writeValueAsString(object) → JSON 문자열
 * - 역직렬화: objectMapper.readValue(json, Type.class) → Java 객체
 * 요청 Body 에 담을 JSON 을 생성할 때 사용합니다.
 */
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
// ...
@WebMvcTest(excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@ActiveProfiles("test")
public abstract class ControllerTestSupport {

    /**
     * HTTP 요청/응답을 시뮬레이션하는 MockMvc 객체
     * perform() 메서드로 가상 HTTP 요청을 보냅니다.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * Java 객체 ↔ JSON 변환기
     * objectMapper.writeValueAsString(dto) 로 요청 바디를 만들 때 사용합니다.
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * AuthService 의 Mock 객체
     * 컨트롤러 테스트에서 실제 DB/Redis 없이 동작을 지정할 수 있습니다.
     *
     * 사용 예:
     * <pre>
     *     given(authService.signup(any())).willReturn(signupResponse);
     * </pre>
     */
    @MockBean
    protected AuthService authService;

    /**
     * UserService 의 Mock 객체
     *
     * 사용 예:
     * <pre>
     *     given(userService.getProfile(anyLong(), anyLong())).willReturn(profileResponse);
     * </pre>
     */
    @MockBean
    protected UserService userService;

    @MockBean
    protected com.seouldate.user.security.GatewayAuthFilter gatewayAuthFilter;
}
