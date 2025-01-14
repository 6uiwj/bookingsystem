package com.bloodDonation.admin.center.controllers;

import com.bloodDonation.admin.center.service.CenterDeleteService;
import com.bloodDonation.admin.menus.Menu;
import com.bloodDonation.admin.menus.MenuDetail;
import com.bloodDonation.admin.center.entities.CenterInfo;
import com.bloodDonation.admin.center.service.CenterInfoService;
import com.bloodDonation.admin.center.service.CenterSaveService;
import com.bloodDonation.commons.ExceptionProcessor;
import com.bloodDonation.commons.ListData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller("adminCenterController")
@RequestMapping("/admin/center")    //url 하나당 하나의 컨트롤러에 매핑되는 다른 핸들러 매핑과 달리 메서드 단위까지 세분화하여 적용 가능. url, 파라미터, 헤더 등.
@RequiredArgsConstructor
public class CenterController implements ExceptionProcessor {

    private final CenterInfoService centerInfoService;
    private final CenterSaveService centerSaveService;
    private final CenterDeleteService centerDeleteService;

    @ModelAttribute("menuCode") //getMenyCode의 리턴값을 Model 객체와 바인딩
    public String getMenuCode() {
        return "center";
    }

    /**
     * 서브메뉴
     * @return
     */
    @ModelAttribute("subMenus")
    public List<MenuDetail> getSubMednus(){
        return Menu.getMenus("center");
    }
    /**
     * 센터 목록
     */
    @GetMapping
    public String list(@ModelAttribute CenterSearch search, Model model) {   // Model 객체: Controller에서 생성된 데이터를 담아 View로 전달할 때 사용
        commonProcess("list", model);

        ListData<CenterInfo> data = centerInfoService.getList(search);

        model.addAttribute("items", data.getItems());
        model.addAttribute("pagination", data.getPagination());

        return "admin/center/list";
    }

    @PatchMapping
    public String editList(@RequestParam(name="chk", required = false) List<Integer> chks, Model model) {
        centerSaveService.saveList(chks);
        model.addAttribute("script", "parent.location.reload()");
        return "common/_execute_script";
    }

    @DeleteMapping
    public String deleteList(@RequestParam(name="chk", required = false) List<Integer> chks, Model model){

        centerDeleteService.deleteList(chks);

        model.addAttribute("script", "parent.location.reload()");
        return "common/_execute_script";
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
     * 센터 수정
     * @param model
     * @return
     */
    @GetMapping("/edit_center/{cCode}")
    public String editCenter(@PathVariable("cCode") Long cCode, Model model) {  // Model 객체를 통해 form 파라미터의 값들을 Getter, Setter, 생성자를 통해 주입, 전달

        commonProcess("edit_center", model);

        RequestCenter form = centerInfoService.getForm(cCode);
        model.addAttribute("requestCenter", form);
        return "admin/center/edit_center";
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

        CenterInfo data = centerSaveService.save(form);

        return "redirect:/admin/center";
    }

    /**
     * 공통 처리
     *
     * @param mode
     * @param model
     */
    private void commonProcess(String mode, Model model) {
        String pageTitle = "헌혈의집 센터 관리";
        mode = Objects.requireNonNullElse(mode, "list");

        List<String> addCommonScript = new ArrayList<>();

        if (mode.equals("add_center") || mode.equals("edit_center")) {
            pageTitle = "센터";
            pageTitle += mode.contains("edit") ? "수정" : "등록";
            addCommonScript.add("address");

        }

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("addCommonScript", addCommonScript);
        model.addAttribute("subMenuCode", mode);
    }
}