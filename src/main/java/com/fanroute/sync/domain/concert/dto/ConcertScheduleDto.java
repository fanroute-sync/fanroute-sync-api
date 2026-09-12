package com.fanroute.sync.domain.concert.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.ScheduleSource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConcertScheduleDto {

  // round는 같은 날짜 안에서 시각순으로 서버가 계산하는 파생값이라 요청에서 받지 않습니다
  // (ConcertSchedule.renumberByTime 참고).
  public record CreateRequest(
      @Schema(description = "대상 공연 ID", example = "1") @NotNull Long concertId,
      @Schema(description = "공연 일자", example = "2026-09-20") @NotNull LocalDate performanceDate,
      @Schema(description = "공연 시각", example = "19:30") @NotNull LocalTime performanceTime) {

  }

  public record UpdateRequest(
      @Schema(description = "공연 일자", example = "2026-09-20") @NotNull LocalDate performanceDate,
      @Schema(description = "공연 시각", example = "19:30") @NotNull LocalTime performanceTime) {

  }

  public record Response(
      @Schema(description = "회차 ID", example = "1") Long id,
      @Schema(description = "대상 공연 ID", example = "1") Long concertId,
      @Schema(description = "회차 번호", example = "1") int round,
      @Schema(description = "공연 일자", example = "2026-09-20") LocalDate performanceDate,
      @Schema(description = "공연 시각", example = "19:30") LocalTime performanceTime,
      @Schema(
          description = "true면 관리자가 아직 확정하지 않은 잠정 값입니다(KOPIS 공연시간 안내를 자동 해석해 채운 값). "
              + "실제 공식 시각이 아닐 수 있으니 화면에 '시각 미정' 등으로 구분해 표시해야 합니다.",
          example = "true") boolean provisional,
      @Schema(description = "KOPIS_PARSED면 자동 생성, MANUAL이면 관리자가 직접 등록·수정한 값")
      ScheduleSource source) {

    public static Response from(ConcertSchedule schedule) {
      return new Response(
          schedule.getId(),
          schedule.getConcert().getId(),
          schedule.getRound(),
          schedule.getPerformanceDate(),
          schedule.getPerformanceTime(),
          schedule.isProvisional(),
          schedule.getSource());
    }
  }
}
