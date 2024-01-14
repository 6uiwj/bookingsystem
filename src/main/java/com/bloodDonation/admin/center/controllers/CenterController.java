package com.bloodDonation.admin.center.controllers;

import com.bloodDonation.commons.ExceptionProcessor;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

@Controller
@RequestMapping("/admin/center")    //url 하나당 하나의 컨트롤러에 매핑되는 다른 핸들러 매핑과 달리 메서드 단위까지 세분화하여 적용 갸능. url, 파라미터, 헤더 등.
public class CenterController implements ExceptionProcessor {

    @ModelAttribute("menuCode") //getMenyCode의 리턴값을 Model 객체와 바인딩
    public String getMenuCode() {
        return "center";
    }

    /**
     * 센터 목록
     */
    @GetMapping
    public String list(Model model) {   // Model 객체: Controller에서 생성된 데이터를 담아 View로 전달할 때 사용
        commonProcess("list", model);

        return "admin/center/list";
    }

    /**
     * 센터 등록 - 등록 & 수정 통합 개발
     * @param model
     * @return
     */
    @GetMapping("/add_center")
    public String addCenter(@ModelAttribute RequestCenter form, Model model) {  // Model 객체를 통해 form 파라미터의 값들을 Getter, Setter, 생성자를 통해 주입, 전달
        commonProcess("add_center", model);

        return "admin/center/add_center";
    }

    /**
     * 센터 추가, 저장
     * @param model
     * @return
     */
    @PostMapping("/save_center")
    public String saveCenter(@Valid RequestCenter form, Errors errors, Model model) {
        String mode = form.getMode();

        commonProcess(mode, model);

        if (errors.hasErrors()) {
            return "admin/center/" + mode;
        }

        return "redirect:/admin/center/info_center";
    }

    @GetMapping("/info_center")
    public String infoCenter(Model model) {
        commonProcess("info_center", model);

        return "admin/center/info_center";
    }

    /**
     * 공통 처리
     *
     * @param mode
     * @param model
     */
    private void commonProcess(String mode, Model model) {
        String pageTitle = "헌혈의집 센터 목록";
        mode = Objects.requireNonNullElse(mode, "list");

        if (mode.equals("add_center")) {
            pageTitle = "새로운 센터 등록";

        } else if (mode.equals("edit_center")) {
            pageTitle = "센터 정보 수정";

        } else if (mode.equals("info_center")) {
            pageTitle = "센터 상세 정보";

        }

        model.addAttribute("pageTitle", pageTitle);
    }
}