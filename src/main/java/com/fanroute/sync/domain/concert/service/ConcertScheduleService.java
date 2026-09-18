package com.fanroute.sync.domain.concert.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.dto.ConcertScheduleDto;
import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.ConcertScheduleRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertScheduleService {

  private final ConcertScheduleRepository concertScheduleRepository;
  // CONCERT_NOT_FOUND 통일을 위해 ConcertRepository를 직접 쓰지 않고 ConcertService에 위임합니다.
  private final ConcertService concertService;
  private final Clock clock;

  public List<ConcertSchedule> getSchedulesByConcert(Long concertId) {
    return concertScheduleRepository.findByConcertId(concertId);
  }

  // round는 저장 후 renumberDate로 재계산하므로 0으로 넘깁니다.
  @Transactional
  public ConcertSchedule createSchedule(ConcertScheduleDto.CreateRequest request) {
    Concert concert = concertService.getConcert(request.concertId());
    validateWithinConcertPeriod(concert, request.performanceDate());

    ConcertSchedule schedule = ConcertSchedule.create(
        concert, 0, request.performanceDate(), request.performanceTime());
    ConcertSchedule saved = withDuplicateTimeHandling(
        () -> concertScheduleRepository.saveAndFlush(schedule));
    renumberDate(concert.getId(), request.performanceDate());
    return saved;
  }

  @Transactional
  public ConcertSchedule updateSchedule(Long scheduleId, ConcertScheduleDto.UpdateRequest request) {
    ConcertSchedule schedule = getSchedule(scheduleId);
    Long concertId = schedule.getConcert().getId();
    LocalDate previousDate = schedule.getPerformanceDate();
    validateWithinConcertPeriod(schedule.getConcert(), request.performanceDate());

    withDuplicateTimeHandling(() -> {
      schedule.update(request.performanceDate(), request.performanceTime());
      concertScheduleRepository.flush();
      return schedule;
    });

    renumberDate(concertId, request.performanceDate());
    if (!previousDate.equals(request.performanceDate())) {
      renumberDate(concertId, previousDate);
    }
    return schedule;
  }

  @Transactional
  public void deleteSchedule(Long scheduleId) {
    ConcertSchedule schedule = getSchedule(scheduleId);
    Long concertId = schedule.getConcert().getId();
    LocalDate date = schedule.getPerformanceDate();
    schedule.delete(clock.instant());
    concertScheduleRepository.flush();
    renumberDate(concertId, date);
  }

  public ConcertSchedule getSchedule(Long scheduleId) {
    return concertScheduleRepository.findWithConcertById(scheduleId)
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.SCHEDULE_NOT_FOUND));
  }

  private void validateWithinConcertPeriod(Concert concert, LocalDate performanceDate) {
    if (performanceDate.isBefore(concert.getStartDate())
        || performanceDate.isAfter(concert.getEndDate())) {
      throw new BusinessException(ConcertErrorCode.SCHEDULE_DATE_OUT_OF_RANGE);
    }
  }

  private void renumberDate(Long concertId, LocalDate date) {
    ConcertSchedule.renumberByTime(
        concertScheduleRepository.findByConcertIdAndPerformanceDate(concertId, date));
  }

  // 현재 위반 가능한 제약은 시각 중복뿐이라 종류를 구분하지 않고 변환합니다. 새 제약 추가 시 재검토 필요.
  private ConcertSchedule withDuplicateTimeHandling(Supplier<ConcertSchedule> action) {
    try {
      return action.get();
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ConcertErrorCode.DUPLICATE_SCHEDULE_TIME);
    }
  }
}
