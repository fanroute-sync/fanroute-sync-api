package com.fanroute.sync.domain.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
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
  @Test
  @DisplayName("AI 생성 작업을 접수하면 실행 큐 처리는 outbox relay에 맡긴다")
  void acceptsGenerationRequestWithoutDirectAsyncExecution() {
    User user = UserFixture.activeUserWithId(1L);
    AiItineraryGenerationDto.CreateResponse response =
        new AiItineraryGenerationDto.CreateResponse(10L, AiItineraryGenerationStatus.PENDING);
    when(currentUserResolver.getCurrentUser(null)).thenReturn(user);
    when(generationService.request(user, 1L)).thenReturn(response);
    assertThat(controller().requestGeneration(null, 1L).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
  }

  private AiItineraryGenerationController controller() {
    return new AiItineraryGenerationController(currentUserResolver, generationService);
  }
}
