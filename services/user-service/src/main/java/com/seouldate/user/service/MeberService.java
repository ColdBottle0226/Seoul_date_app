package com.seouldate.user.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MeberService {
    
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public MemberResponse register(ReqisterRequest request){
        // 중복 이메일 검증
        if(memberRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }

        // 비밀번호 암호화 후 저장 
        Member member = Member.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();

        Member savedMember = memberRepository.save(member);

        return MemberResponse.from(savedMember);
    }

    // 로그인 
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request){
        Member member = memberRepository.findByEmail(request.getEmail())
            .orElseThrow(MemberNotFoundException::new);

        if(!passwordEncoder.matches(request.getPassword(), member.getPassword())){
            throw new InvalidPasswordException();
        }

        return LoginResponse.of(member);        
    }

}
