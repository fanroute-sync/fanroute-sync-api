package com.fanroute.sync.domain.concert.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.ConcertDto;
import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.service.ConcertService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ConcertController implements ConcertApi {

  private final ConcertService concertService;

  @Override
  public ResponseEntity<ApiResponse<Page<ConcertDto.Response>>> getConcerts(
      String genreName, int page, int size) {
    Genre genre = parseGenre(genreName);
    Pageable pageable = toPageable(page, size);
    Page<ConcertDto.Response> concerts =
        concertService.getConcerts(genre, pageable).map(ConcertDto.Response::from);
    return ApiResponse.ok(concerts).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<ConcertDto.Response>> getConcert(Long concertId) {
    Concert concert = concertService.getConcert(concertId);
    return ApiResponse.ok(ConcertDto.Response.from(concert)).toResponseEntity();
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
}
