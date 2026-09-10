package com.fanroute.sync.domain.concert.batch;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fanroute.sync.domain.concert.entity.ScheduleParseStatus;

/** KOPIS dtguidance 원문을 "요일(범위)(시각,시각...)" 패턴으로만 엄격하게(all-or-nothing) 해석합니다.
 * 일부만 맞는 결과를 저장하면 틀린 시각을 실제 공연 시각으로 오인시킬 수 있기 때문입니다. */
public final class PerformanceTimeGuideParser {

  /** 파서 로직이 바뀌면 올려서 이미 처리된 공연도 재파싱 대상으로 잡습니다. */
  public static final int VERSION = 1;

  // 장기 오픈런 등 이상치 방어 — 전개 결과가 이보다 많으면 전체를 실패로 취급합니다.
  private static final int MAX_GENERATED_SLOTS = 250;

  private static final DayOfWeek[] KOREAN_DAY_ORDER = {
      DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
      DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
  };

  private static final Pattern SEGMENT = Pattern.compile(
      "([월화수목금토일])요일(?:\\s*~\\s*([월화수목금토일])요일)?\\((\\d{2}:\\d{2}(?:,\\d{2}:\\d{2})*)\\)");
  private static final Pattern SEPARATOR = Pattern.compile("^,\\s*$");
  private static final Pattern TRAILING_BLANK = Pattern.compile("^\\s*$");

  private PerformanceTimeGuideParser() {
  }

  public record GeneratedSlot(LocalDate date, LocalTime time) {
  }

  public record GenerationResult(ScheduleParseStatus status, List<GeneratedSlot> slots) {

    private static GenerationResult of(ScheduleParseStatus status) {
      return new GenerationResult(status, List.of());
    }
  }

  private record WeeklySlot(Set<DayOfWeek> daysOfWeek, List<LocalTime> times) {
  }

  public static GenerationResult generate(String guide, LocalDate startDate, LocalDate endDate) {
    if (guide == null || guide.isBlank()) {
      return GenerationResult.of(ScheduleParseStatus.NO_SCHEDULE);
    }

    List<WeeklySlot> weeklySlots = parseStrictly(guide);
    if (weeklySlots == null) {
      return GenerationResult.of(ScheduleParseStatus.UNSUPPORTED_FORMAT);
    }

    // 요일 범위가 겹치거나 같은 세그먼트가 반복돼도 동일 회차를 두 번 저장하지 않습니다.
    Set<GeneratedSlot> generated = new LinkedHashSet<>();
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      DayOfWeek dayOfWeek = date.getDayOfWeek();
      for (WeeklySlot slot : weeklySlots) {
        if (!slot.daysOfWeek().contains(dayOfWeek)) {
          continue;
        }
        for (LocalTime time : slot.times()) {
          generated.add(new GeneratedSlot(date, time));
        }
      }
    }

    if (generated.size() > MAX_GENERATED_SLOTS) {
      return GenerationResult.of(ScheduleParseStatus.LIMIT_EXCEEDED);
    }

    List<GeneratedSlot> sorted = new ArrayList<>(generated);
    sorted.sort(Comparator.comparing(GeneratedSlot::date).thenComparing(GeneratedSlot::time));
    return new GenerationResult(ScheduleParseStatus.PARSED, sorted);
  }

  /** 문자열 전체가 "세그먼트(,\s*세그먼트)*" 형태로 빈틈없이 구성될 때만 세그먼트 목록을 반환하고, 아니면 null. */
  private static List<WeeklySlot> parseStrictly(String guide) {
    Matcher matcher = SEGMENT.matcher(guide);
    List<WeeklySlot> slots = new ArrayList<>();
    int expectedStart = 0;

    while (matcher.find()) {
      if (matcher.start() < expectedStart) {
        return null;
      }
      if (matcher.start() != expectedStart
          && !SEPARATOR.matcher(guide.substring(expectedStart, matcher.start())).matches()) {
        return null;
      }

      WeeklySlot slot = toWeeklySlot(matcher);
      if (slot == null) {
        return null;
      }
      slots.add(slot);
      expectedStart = matcher.end();
    }

    if (slots.isEmpty() || !TRAILING_BLANK.matcher(guide.substring(expectedStart)).matches()) {
      return null;
    }
    return slots;
  }

  private static WeeklySlot toWeeklySlot(Matcher matcher) {
    DayOfWeek fromDay = dayOfWeekOf(matcher.group(1));
    String toDayToken = matcher.group(2);
    Set<DayOfWeek> days;
    if (toDayToken == null) {
      days = EnumSet.of(fromDay);
    } else {
      DayOfWeek toDay = dayOfWeekOf(toDayToken);
      days = expandRange(fromDay, toDay);
      if (days == null) {
        return null;
      }
    }

    List<LocalTime> times = new ArrayList<>();
    for (String token : matcher.group(3).split(",")) {
      times.add(LocalTime.parse(token));
    }
    return new WeeklySlot(days, times);
  }

  // "토~화" 같은 주 경계를 넘는 표현은 실사용을 확인하지 못해 임의로 추측하지 않고 실패 처리합니다.
  private static Set<DayOfWeek> expandRange(DayOfWeek from, DayOfWeek to) {
    int fromIndex = indexOf(from);
    int toIndex = indexOf(to);
    if (toIndex < fromIndex) {
      return null;
    }
    Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    for (int i = fromIndex; i <= toIndex; i++) {
      days.add(KOREAN_DAY_ORDER[i]);
    }
    return days;
  }

  private static int indexOf(DayOfWeek day) {
    for (int i = 0; i < KOREAN_DAY_ORDER.length; i++) {
      if (KOREAN_DAY_ORDER[i] == day) {
        return i;
      }
    }
    throw new IllegalStateException("unreachable");
  }

  private static DayOfWeek dayOfWeekOf(String korean) {
    return switch (korean) {
      case "월" -> DayOfWeek.MONDAY;
      case "화" -> DayOfWeek.TUESDAY;
      case "수" -> DayOfWeek.WEDNESDAY;
      case "목" -> DayOfWeek.THURSDAY;
      case "금" -> DayOfWeek.FRIDAY;
      case "토" -> DayOfWeek.SATURDAY;
      case "일" -> DayOfWeek.SUNDAY;
      default -> throw new IllegalStateException("unreachable: " + korean);
    };
  }

  public static String hash(String guide, LocalDate startDate, LocalDate endDate) {
    String source = (guide == null ? "" : guide) + '|' + startDate + '|' + endDate;
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 must be available on every JVM", e);
    }
  }
}
