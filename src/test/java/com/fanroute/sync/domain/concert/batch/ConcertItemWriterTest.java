package com.fanroute.sync.domain.concert.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.ScheduleParseStatus;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.ConcertScheduleRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class ConcertItemWriterTest {

  @Mock
  private VenueRepository venueRepository;
  @Mock
  private ConcertRepository concertRepository;
  @Mock
  private ConcertScheduleRepository concertScheduleRepository;

  private ConcertItemWriter writer;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC);
    writer = new ConcertItemWriter(
        venueRepository, concertRepository, concertScheduleRepository, clock);
    when(venueRepository.findByKopisVenueId("FC001")).thenReturn(Optional.empty());
    when(venueRepository.save(any(Venue.class)))
        .thenReturn(Venue.create("FC001", "테스트홀", "부산", 35.1, 129.0));
  }

  @Test
  @DisplayName("신규 공연 + 파싱 가능한 안내면 공연 기간 안의 실제 날짜별 회차가 생성된다")
  void savesNewConcertAndGeneratesSchedulesWhenGuideParses() {
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.empty());
    when(concertRepository.save(any(Concert.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // 2026-09-01(화)~09-02(수) 기간, "화요일(20:00)"만 매칭 → 9/1 20:00 1건만 생성돼야 한다.
    writer.write(new Chunk<>(draft("화요일(20:00)")));

    ArgumentCaptor<Concert> concertCaptor = ArgumentCaptor.forClass(Concert.class);
    verify(concertRepository).save(concertCaptor.capture());
    assertThat(concertCaptor.getValue().getScheduleParseStatus())
        .isEqualTo(ScheduleParseStatus.PARSED);

    ArgumentCaptor<List<ConcertSchedule>> savedCaptor = ArgumentCaptor.forClass(List.class);
    verify(concertScheduleRepository).saveAll(savedCaptor.capture());
    List<ConcertSchedule> saved = savedCaptor.getValue();
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).getPerformanceDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(saved.get(0).getPerformanceTime()).isEqualTo(LocalTime.of(20, 0));
    assertThat(saved.get(0).getRound()).isEqualTo(1);
    assertThat(saved.get(0).isProvisional()).isTrue();
  }

  @Test
  @DisplayName("공연시간 안내가 없으면 회차를 만들지 않고 NO_SCHEDULE 상태만 남긴다")
  void doesNotGenerateSchedulesWhenGuideIsMissing() {
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.empty());
    when(concertRepository.save(any(Concert.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    writer.write(new Chunk<>(draft(null)));

    ArgumentCaptor<Concert> concertCaptor = ArgumentCaptor.forClass(Concert.class);
    verify(concertRepository).save(concertCaptor.capture());
    assertThat(concertCaptor.getValue().getScheduleParseStatus())
        .isEqualTo(ScheduleParseStatus.NO_SCHEDULE);
    verify(concertScheduleRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("안내 형식을 해석할 수 없으면 임의의 시각을 만들지 않고 상태만 기록한다")
  void doesNotGenerateSchedulesWhenGuideIsUnsupported() {
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.empty());
    when(concertRepository.save(any(Concert.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    writer.write(new Chunk<>(draft("홈페이지 참고")));

    ArgumentCaptor<Concert> concertCaptor = ArgumentCaptor.forClass(Concert.class);
    verify(concertRepository).save(concertCaptor.capture());
    assertThat(concertCaptor.getValue().getScheduleParseStatus())
        .isEqualTo(ScheduleParseStatus.UNSUPPORTED_FORMAT);
    verify(concertScheduleRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("안내가 해석 불가능하게 바뀌면 근거를 잃은 기존 자동 회차를 비활성화한다")
  void deletesAutoSchedulesWhenGuideBecomesUnsupported() {
    Concert existing = existingConcertWithGuide("화요일(20:00)");
    ConcertSchedule oldAuto = ConcertSchedule.createFromParsedSlot(
        existing, 1, LocalDate.of(2026, 9, 1), LocalTime.of(20, 0));
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.of(existing));
    when(concertScheduleRepository.findByConcertId(any())).thenReturn(List.of(oldAuto));

    writer.write(new Chunk<>(draft("홈페이지 참고")));

    assertThat(oldAuto.isDeleted()).isTrue();
    assertThat(existing.getScheduleParseStatus())
        .isEqualTo(ScheduleParseStatus.UNSUPPORTED_FORMAT);
    verify(concertScheduleRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("원문·기간이 지난 시도와 같으면 재파싱도 조회도 하지 않는다")
  void skipsReconciliationWhenNothingChanged() {
    Concert existing = existingConcertWithGuide("화요일(20:00)");
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.of(existing));

    writer.write(new Chunk<>(draft("화요일(20:00)")));

    verify(concertScheduleRepository, never()).findByConcertId(any());
    verify(concertScheduleRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("안내가 바뀌면 기존 자동·미확정 회차만 지우고 새로 전개한 회차로 교체한다")
  void replacesAutoUnconfirmedSchedulesWhenGuideChanges() {
    Concert existing = existingConcertWithGuide("화요일(19:00)"); // 이전 안내 — 이번엔 다른 시각
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.of(existing));

    ConcertSchedule oldAuto = ConcertSchedule.createFromParsedSlot(
        existing, 1, LocalDate.of(2026, 9, 1), LocalTime.of(19, 0));
    when(concertScheduleRepository.findByConcertId(any())).thenReturn(List.of(oldAuto));

    writer.write(new Chunk<>(draft("화요일(20:00)")));

    assertThat(oldAuto.isDeleted()).isTrue();
    verify(concertScheduleRepository).flush();
    ArgumentCaptor<List<ConcertSchedule>> savedCaptor = ArgumentCaptor.forClass(List.class);
    verify(concertScheduleRepository).saveAll(savedCaptor.capture());
    assertThat(savedCaptor.getValue()).hasSize(1);
    assertThat(savedCaptor.getValue().get(0).getPerformanceTime()).isEqualTo(LocalTime.of(20, 0));
  }

  @Test
  @DisplayName("관리자가 확정한 회차와 겹치는 시각은 자동 생성에서 건너뛰고 확정 회차는 그대로 둔다")
  void skipsSlotsConflictingWithConfirmedSchedule() {
    Concert existing = existingConcertWithGuide(null); // 이전엔 안내가 없었음 — 이번에 새로 생김
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.of(existing));

    ConcertSchedule confirmed = ConcertSchedule.create(
        existing, 1, LocalDate.of(2026, 9, 1), LocalTime.of(20, 0));
    when(concertScheduleRepository.findByConcertId(any())).thenReturn(List.of(confirmed));

    writer.write(new Chunk<>(draft("화요일(20:00)")));

    verify(concertScheduleRepository, never()).saveAll(any());
    assertThat(confirmed.getPerformanceTime()).isEqualTo(LocalTime.of(20, 0)); // 손대지 않음
  }

  private Concert existingConcertWithGuide(String guide) {
    Venue venue = Venue.create("FC001", "테스트홀", "부산", 35.1, 129.0);
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2), null, guide, Instant.parse("2020-01-01T00:00:00Z"));
    String hash = PerformanceTimeGuideParser.hash(
        guide, concert.getStartDate(), concert.getEndDate());
    concert.updateScheduleParseState(
        guide == null ? ScheduleParseStatus.NO_SCHEDULE : ScheduleParseStatus.PARSED,
        PerformanceTimeGuideParser.VERSION, hash);
    return concert;
  }

  private ConcertSyncDraft draft(String performanceTimeGuide) {
    return new ConcertSyncDraft(
        "PF001", "테스트 공연", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2),
        Genre.POPULAR_MUSIC, "poster.jpg", performanceTimeGuide, "FC001", "테스트홀", "부산", 35.1,
        129.0);
  }
}
