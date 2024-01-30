package com.bloodDonation.reservation.service;

import com.bloodDonation.admin.center.entities.CenterInfo;
import com.bloodDonation.admin.center.service.CenterInfoService;
import com.bloodDonation.commons.ListData;
import com.bloodDonation.commons.Pagination;
import com.bloodDonation.commons.Utils;
import com.bloodDonation.member.MemberUtil;
import com.bloodDonation.reservation.controllers.RequestReservation;
import com.bloodDonation.reservation.controllers.ReservationSearch;
import com.bloodDonation.reservation.entities.QReservation;
import com.bloodDonation.reservation.entities.Reservation;
import com.bloodDonation.reservation.repositories.ReservationRepository;
import com.querydsl.core.BooleanBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.springframework.data.domain.Sort.Order.desc;

@Service
@RequiredArgsConstructor
public class ReservationInfoService {
    private final ReservationRepository reservationRepository;
    private final CenterInfoService centerInfoService;
    private final HttpServletRequest request;
    private final MemberUtil memberUtil;



    public Reservation get(Long bookCode) {
        Reservation reservation = reservationRepository.findById(bookCode).orElseThrow(ReservationNotFoundException::new);

        //추가작업 필요

        return reservation;
    }

    public RequestReservation getForm(Long bookCode) {
        Reservation reservation = get(bookCode);
        RequestReservation form = new ModelMapper().map(reservation, RequestReservation.class);

        form.setStatus(reservation.getStatus().name());
        form.setBookType(reservation.getBookType().name());

        LocalDateTime bookDateTime = reservation.getBookDateTime();
        form.setDate(bookDateTime.toLocalDate());
        form.setTime(bookDateTime.toLocalTime());
        form.setPersons(reservation.getCapacity());
        form.setCCode(reservation.getCenter().getCCode());
        form.setMember(reservation.getMember());

        return form;
    }

    public ListData<Reservation> getList(ReservationSearch search) {
        QReservation reservation = QReservation.reservation;

        BooleanBuilder andBuilder = new BooleanBuilder();

        //검색 조건 키워드들
        List<Long> memberSeq = search.getMemberSeq();
        List<Long> bookCode = search.getBookCode();
        List<String> userId = search.getUserId();
        LocalDate sDate = search.getSDate();
        LocalDate eDate = search.getEDate();


        //회원번호로 조회
        if(memberSeq != null && !memberSeq.isEmpty()) {
            andBuilder.and(reservation.member.userNo.in(memberSeq));
        }

        if(bookCode != null && !bookCode.isEmpty()) {
            andBuilder.and(reservation.bookCode.in(bookCode));
        }

        if(userId != null && !userId.isEmpty()) {
            andBuilder.and(reservation.member.userId.in(userId));
        }

        if(sDate != null) {
            andBuilder.and(reservation.bookDateTime.goe(LocalDateTime.of(
                    sDate, LocalTime.of(0,0,0))));
        }

        if(eDate != null) {
            andBuilder.and(reservation.bookDateTime.loe(LocalDateTime.of(eDate, LocalTime.of(23,59,59))));

        }

        //검색 조건 페이징

        int page = Utils.onlyPositiveNumber(search.getPage(),1);
        int limit = Utils.onlyPositiveNumber(search.getLimit(),20);

        Pageable pageable = PageRequest.of(page, limit, Sort.by(desc("createdAt")));
        Page<Reservation> data = reservationRepository.findAll(andBuilder, pageable);

        Pagination pagination = new Pagination(page, (int)data.getTotalElements(), 10, limit, request);

        return new ListData<>(data.getContent(), pagination);
    }

    //마이페이지 회원 예약조회에 이용할 메서드! 
    public ListData<Reservation> getMyList(ReservationSearch search) {
        //로그인정보가 없으면 null 출력
        if (!memberUtil.isLogin()) {
            return null;
        }
        //로그인 정보가 있으면 가져와서 그 로그인한 회원의 userId에만 해당하는 예약 데이터출력
        search.setUserId(Arrays.asList(memberUtil.getMember().getUserId()));

        return getList(search);
    }

    public int getAvailableCapacity(Long cCode, LocalDateTime bookDateTime) {
        CenterInfo center = centerInfoService.get(cCode);
        Integer current = reservationRepository.getTotalCapacity(cCode, bookDateTime);
        int curr = Objects.requireNonNullElse(current, 0);

        return center.getBookCapacity() - curr;
    }

}
