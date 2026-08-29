package com.fanroute.sync.domain.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;

import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.service.AiItineraryGenerationAsyncService;
import com.fanroute.sync.domain.schedule.service.AiItineraryGenerationService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class AiItineraryGenerationControllerTest {

  @Mock
  private CurrentUserResolver currentUserResolver;
  @Mock
  private AiItineraryGenerationService generationService;
  @Mock
  private AiItineraryGenerationAsyncService generationAsyncService;

  @Test
  @DisplayName("실행기 거절 시 생성 작업을 실패 상태로 남긴다")
  void failsGenerationWhenExecutorRejectsRequest() {
    User user = UserFixture.activeUserWithId(1L);
    AiItineraryGenerationDto.CreateResponse response =
        new AiItineraryGenerationDto.CreateResponse(10L, AiItineraryGenerationStatus.PENDING);
    when(currentUserResolver.getCurrentUser(null)).thenReturn(user);
    when(generationService.request(user, 1L)).thenReturn(response);
    doThrow(new TaskRejectedException("queue full")).when(generationAsyncService).generate(10L);

    assertThat(controller().requestGeneration(null, 1L).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    verify(generationService).fail(10L);
  }

  private AiItineraryGenerationController controller() {
    return new AiItineraryGenerationController(currentUserResolver, generationService,
        generationAsyncService);
  }
}
