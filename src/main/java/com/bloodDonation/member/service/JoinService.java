package com.bloodDonation.member.service;


import com.bloodDonation.member.Authority;
import com.bloodDonation.member.controllers.JoinValidator;
import com.bloodDonation.member.controllers.RequestJoin;
import com.bloodDonation.member.entities.Authorities;
import com.bloodDonation.member.entities.Member;
import com.bloodDonation.member.repositories.AuthoritiesRepository;
import com.bloodDonation.member.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

@Service
@RequiredArgsConstructor
@Transactional
public class JoinService {

    private final MemberRepository memberRepository; //회원가입 성공시 DB저장
    private final JoinValidator validator; //검증추가
    private final PasswordEncoder encoder; //비밀번호 해시화
    private final AuthoritiesRepository authoritiesRepository;
    //커맨드 객체 형태로 가입이 들어올 경우
    public void process(RequestJoin form, Errors errors){
        validator.validate(form, errors);

        //실패하면 로직을 수행하지 않고 return 으로 종료
        if(errors.hasErrors()){
            return;
        }

        //비밀번호 BCrypt로 해시화
        String hash = encoder.encode(form.getUserPw());
        //커맨드 객체 form 을 변환메소드 map이 멤버 정보를 비교해서 해시화된 비번만 넣어줌
        Member member = new Member();
        member.setEmail(form.getEmail());
        member.setMName(form.getMName());
        //비밀번호 확인하려고 잠깐 hash 대신 넣음
        member.setUserPw(form.getUserPw());
        member.setUserId(form.getUserId());

        //DB에 저장
        process(member);

        //저장 후, 회원가입시에 일반사용자로 권한 부여
        Authorities authorities = new Authorities();
        authorities.setMember(member);
        authorities.setAuthority(Authority.USER);
        authoritiesRepository.saveAndFlush(authorities);
    }

    public void process(Member member){
        memberRepository.saveAndFlush(member);

    }
}
