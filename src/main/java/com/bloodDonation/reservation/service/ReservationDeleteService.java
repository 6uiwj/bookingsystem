package com.bloodDonation.reservation.service;

import com.bloodDonation.commons.Utils;
import com.bloodDonation.commons.exceptions.AlertException;
import com.bloodDonation.reservation.entities.Reservation;
import com.bloodDonation.reservation.repositories.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationDeleteService {
    private final ReservationRepository repository;
    private final ReservationInfoService infoService;
    private final Utils utils;

    public void deleteList(List<Integer> chks) {
        if (chks == null || chks.isEmpty()) {
            throw new AlertException("삭제할 예약을 선택하세요.", HttpStatus.BAD_REQUEST);
        }

        for(int chk : chks) {
            Long bookCode = Long.valueOf(utils.getParam("bookCode_" + chk));
            Reservation reservation = repository.findById(bookCode).orElse(null);
            if(reservation == null) {
                continue;
            }

            repository.delete(reservation);
        }
        repository.flush();

    }

    public void delete(Long bookCode) {
        Reservation reservation = infoService.get(bookCode);

        repository.delete(reservation);
        repository.flush();
    }
}
