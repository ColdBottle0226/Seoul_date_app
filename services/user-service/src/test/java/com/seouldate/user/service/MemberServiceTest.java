
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    // ======== 회원가입 테스트 ========
    @Test
    @DisplayName("[Red] 정상적인 회원가입 요청 시, 저장된 회원정보 반환")
    void regist_validRequest_returnsSavedMember() {
        // Given
        RegisterRequest request = new RegisterRequest("chan@test.com", "pass123!");
        Member savedMember = Member.builder()
                .id(1L)
                .email("chan@test.com")
                .password("encoded_password")
                .build();

        given(memberRepository.existsByEmail("chan@test.com")).willReturn(false);
        given(passwordEncoder.encode("pass123!")).willReturn("encoded_password");
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        // When
        MemberResponse response = memberService.register(request);

        // Then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("chan@test.com");

    }

    @Test
    @DisplayName("[Red] 이미 가입된 이메일로 요청 시, DuplicateEmailException 발생")
    void register_duplicateEmail_throwsDuplicateEmailException() {
        // Given
        RegisterRequest request = new RegisterRequest("chan@test.com", "pass123!");
        given(memberRepository.existsByEmail("chan@test.com")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 가입된 이메일입니다.");

        then(memberRepository).should(never()).save(any(Member.class));
    }
    
    @Test
    @DisplayName("[Red] 비밀번호는 암호화되어 저장된다")
    void register_passwordIsEncoded(){
        // Given
        RegisterRequest request = new RegisterRequest("chan@test.com", "pass123!");
        given(memberRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode("raw_password")).willReturn("encoded_password");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            assertThat(member.getPassword()).isEqualTo("hashed_password"); // 저장 시 인코딩 검증
            return member;
        });

        // When
        memberService.register(request);
        
        // Then
        then(passwordEncoder).should().encode(request.getPassword());            
    }


    // ======== 로그인 테스트 =======
    @Test
    @DisplayName("[Red] 존재하지 않는 이메일로 로그인 시 MemberNotFoundException 발생")
    void login_emailNotFound_throwsMemberNotFoundException(){
        // Given
        LoginRequest request = new LoginRequest("notExist@test.com", "pass123!");
        given(memberRepository.findByEmail("notExist@test.com")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(MemberNotFoundException.class)
    }

    @Test
    @DisplayName("[Red] 비밀번호 불일치 시, InvalidPasswordException 발생")
    void login_invalidPassword_throwsInvalidPasswordException(){
        // Given
        LoginRequest request = new LoginRequest("chan@test.com", "wrong_password");
        Member member = Member.builder()
                .email("chan@test.com")
                .password("encoded_password")
                .build();
            
        // 존재하는 회원
        given(memberRepository.findByEmail("chan@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong_password", "encoded_password")).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(InvalidPasswordException.class)
    }


}