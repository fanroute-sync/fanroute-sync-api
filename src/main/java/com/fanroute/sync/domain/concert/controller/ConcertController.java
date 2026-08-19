package com.fanroute.sync.domain.concert.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.ConcertDto;
import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.service.ConcertService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
@Tag(name = "공연", description = "부산 공연 목록 및 상세 조회 API")
public class ConcertController {

  private static final int MAX_PAGE_SIZE = 100;

  private final ConcertService concertService;

  @Operation(summary = "공연 목록 조회", description = "종료되지 않은 공연을 공연 시작일 오름차순으로 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "공연 목록 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "INVALID_GENRE")
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ConcertDto.Response>>> getConcerts(
      @Parameter(description = "장르명으로 필터링. 생략 시 전체 장르 조회")
      @Schema(allowableValues = {
          "연극", "무용(서양/한국무용)", "대중무용", "서양음악(클래식)", "한국음악(국악)", "대중음악", "복합", "서커스/마술",
          "뮤지컬"})
      @RequestParam(required = false) String genreName,
      @Parameter(description = "페이지 번호(0부터 시작)")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기(최대 " + MAX_PAGE_SIZE + ")")
      @RequestParam(defaultValue = "20") int size) {
    Genre genre = parseGenre(genreName);
    Pageable pageable = toPageable(page, size);
    Page<ConcertDto.Response> concerts =
        concertService.getConcerts(genre, pageable).map(ConcertDto.Response::from);
    return ApiResponse.ok(concerts).toResponseEntity();
  }

  private Genre parseGenre(String genreName) {
    if (genreName == null || genreName.isBlank()) {
      return null;
    }
    try {
      return Genre.fromLabel(genreName);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ConcertErrorCode.INVALID_GENRE);
    }
  }

  private Pageable toPageable(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "startDate"));
  }

  @Operation(summary = "공연 상세 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "공연 상세 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "CONCERT_NOT_FOUND")
  @GetMapping("/{concertId}")
  public ResponseEntity<ApiResponse<ConcertDto.Response>> getConcert(
      @PathVariable Long concertId) {
    Concert concert = concertService.getConcert(concertId);
    return ApiResponse.ok(ConcertDto.Response.from(concert)).toResponseEntity();
  }
}
