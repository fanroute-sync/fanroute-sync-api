package com.fanroute.sync.domain.concert.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.concert.dto.ConcertDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** {@link ConcertController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1/concerts")
@Tag(name = "공연", description = "부산 공연 목록 및 상세 조회 API")
public interface ConcertApi {

  int MAX_PAGE_SIZE = 100;

  @Operation(summary = "공연 목록 조회", description = "종료되지 않은 공연을 공연 시작일 오름차순으로 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "공연 목록 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "INVALID_GENRE")
  @GetMapping
  ResponseEntity<ApiResponse<Page<ConcertDto.Response>>> getConcerts(
      @Parameter(description = "장르명으로 필터링. 생략 시 전체 장르 조회")
      @Schema(allowableValues = {
          "연극", "무용(서양/한국무용)", "대중무용", "서양음악(클래식)", "한국음악(국악)", "대중음악", "복합", "서커스/마술",
          "뮤지컬"})
      @RequestParam(required = false) String genreName,
      @Parameter(description = "페이지 번호(0부터 시작)")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기(최대 " + MAX_PAGE_SIZE + ")")
      @RequestParam(defaultValue = "20") int size);

  @Operation(summary = "공연 상세 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "공연 상세 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "CONCERT_NOT_FOUND")
  @GetMapping("/{concertId}")
  ResponseEntity<ApiResponse<ConcertDto.Response>> getConcert(@PathVariable Long concertId);
}
