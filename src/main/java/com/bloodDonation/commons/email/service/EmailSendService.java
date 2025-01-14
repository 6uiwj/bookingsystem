package com.bloodDonation.commons.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmailSendService {
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    /**
     * 메일 전송
     *
     * @param message : 수신인, 메일 제목, 메일 내용
     * @param tpl : 템플릿 사용하는 경우 템플릿 이름
     * @param tplData : 치환코드
     * @return
     */
    //메세지  + 파일첨부 + 추가데이터 같이 보내고싶을때
    public boolean sendMail(EmailMessage message, String tpl, Map<String, Object> tplData) {
        String text = null;
        /**
         * 이메일 템플릿 사용하는 경우 EmailMessage의 제목, 내용, 수신인 및 tplData 추가
         * 치환 속성 전달하고
         * 타임리프로 번역된 텍스트를 반환 값으로 처리
         */
        if (StringUtils.hasText(tpl)) {
            //추가데이터
            tplData = Objects.requireNonNullElse(tplData, new HashMap<>());
            Context context = new Context();

            tplData.put("to", message.to());
            tplData.put("subject", message.subject());
            tplData.put("message", message.message());

            context.setVariables(tplData);

            text = templateEngine.process("email/" + tpl, context);
        } else { // 템플릿 전송이 아닌 경우 메세지로 대체
            text = message.message();
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            //파일점부 활성화 multipart : true
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(message.to()); // 메일 수신자
            mimeMessageHelper.setSubject(message.subject());  // 메일 제목
            mimeMessageHelper.setText(text, true); // 메일 내용 html 형태로 / false : 텍스트형태
            javaMailSender.send(mimeMessage);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return false;

    }
    //tplData : 추가데이터
    //간단하게 메세지만 보내는 경우
    public boolean sendMail(EmailMessage message) {
        return sendMail(message, null, null); //파일첨부 x , 추가데이터 x
    }
}




