package com.bloodDonation.member.controllers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


//비밀번호 찾기 양식 관련 커맨드 객체 추가
public record RequestFindPw(
        @NotBlank @Email
        String email,

        @NotBlank
        String mName
) {
}
