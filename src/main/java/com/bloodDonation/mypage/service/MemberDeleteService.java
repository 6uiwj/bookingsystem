package com.bloodDonation.mypage.service;

import com.bloodDonation.member.MemberUtil;
import com.bloodDonation.member.entities.Member;
import com.bloodDonation.member.repositories.MemberRepository;
import com.bloodDonation.mypage.controllers.RequestUnRegister;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberDeleteService {
    private final MemberRepository memberRepository;

    private final MemberUtil memberUtil;
    private final HttpSession session;

    public void delete(RequestUnRegister form) {
        if (!memberUtil.isLogin()) {
            return;
        }

        Member member = memberUtil.getMember();//memberutil에 session에 저장된 내 정보를 가져오는 getmember이용하여 member에 저장
        /*member = memberRepository.findByUserId(member.getUserId()).orElse(null);//기본값으로 널 넣기*/


        if(member.getUserPw().equals(form.getUserPw())){
            member.setEnable(false);
            member.setMName("****");
            member.setUserPw("****");
            memberRepository.saveAndFlush(member);

        }


        //db에서 삭제된 멤버데이터를 세션에 다시 set(update)
        session.invalidate(); // 로그아웃
        //memberRepository.delete(member);
    }
}
