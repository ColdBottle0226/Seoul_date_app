package com.seouldate.user.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seouldate.user.config.SecurityConfig;
import com.seouldate.user.security.GatewayAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * 컨트롤러 슬라이스 테스트 공통 지원 클래스.
 *
 * <p>사용법:
 * <pre>{@code
 * @WebMvcTest(AuthController.class)
 * class AuthControllerTest extends ControllerTestSupport {
 *     @MockBean AuthService authService;
 *     ...
 * }
 * }</pre>
 *
 * <p>보안 설정({@link SecurityConfig}, {@link GatewayAuthFilter})을 함께 로드하여
 * X-User-Seq / X-Internal-Service 헤더 검증까지 테스트한다.
 */
@ActiveProfiles("test")
@Import({SecurityConfig.class, GatewayAuthFilter.class})
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // 공통 헤더 상수
    // ─────────────────────────────────────────────────────────────────────────

    /** Gateway 가 주입하는 사용자 식별 헤더 */
    protected static final String HEADER_USER_SEQ  = "X-User-Seq";
    protected static final String HEADER_USER_ROLE = "X-User-Role";

    /** 내부 서비스 간 통신 헤더 */
    protected static final String HEADER_INTERNAL_SERVICE = "X-Internal-Service";

    // ─────────────────────────────────────────────────────────────────────────
    // 헬퍼 메서드
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gateway 인증 헤더를 생성한다.
     *
     * <pre>{@code
     * mockMvc.perform(get("/api/users/me").headers(authHeader(1001L, "USER")))
     * }</pre>
     */
    protected HttpHeaders authHeader(long userSeq, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_SEQ, String.valueOf(userSeq));
        headers.set(HEADER_USER_ROLE, role);
        return headers;
    }

    /** 내부 서비스 요청 헤더를 생성한다. */
    protected HttpHeaders internalHeader(String serviceName) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_INTERNAL_SERVICE, serviceName);
        return headers;
    }

    /** 요청에 JSON Content-Type 과 인증 헤더를 추가한다. */
    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request,
                                                     long userSeq, String role) {
        return request
                .headers(authHeader(userSeq, role))
                .contentType(MediaType.APPLICATION_JSON);
    }

    /** 요청에 JSON Content-Type 과 내부 서비스 헤더를 추가한다. */
    protected MockHttpServletRequestBuilder withInternal(MockHttpServletRequestBuilder request,
                                                         String serviceName) {
        return request
                .headers(internalHeader(serviceName))
                .contentType(MediaType.APPLICATION_JSON);
    }

    /** 객체를 JSON 문자열로 직렬화한다. */
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /** ResultActions 에 print() 를 추가하고 반환 (디버깅 편의) */
    protected ResultActions andPrint(ResultActions actions) throws Exception {
        return actions.andDo(print());
    }
}
