package com.fanroute.sync.domain.schedule.client;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fanroute.sync.domain.schedule.config.GeminiProperties;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;

import tools.jackson.databind.json.JsonMapper;

/** 실제 API 호출 없이 현재 클라이언트가 만드는 요청을 비교 실험에 내보낸다. */
@EnabledIfEnvironmentVariable(named = "AI_EVALUATION_EXPORT", matches = "true")
class GeminiEvaluationFixtureTest {

  @Test
  void exportsCurrentRequests() throws Exception {
    JsonMapper mapper = JsonMapper.builder().build();
    List<Map<String, Object>> fixtures = new ArrayList<>();
    for (var scenario : mapper.readTree(Files.readString(Path.of("tools/ai-evaluation/scenarios.json")))) {
      var input = mapper.treeToValue(scenario.path("input"), AiItineraryGenerationDto.GenerationInput.class);
      RestClient.Builder builder = RestClient.builder().baseUrl("https://gemini.example.com");
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server.expect(request -> {
        var body = mapper.readTree(((MockClientHttpRequest) request).getBodyAsString());
        fixtures.add(Map.of("name", scenario.path("name").asText(), "input", input, "request", body));
      }).andRespond(withSuccess("""
          {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"{\\"items\\":[]}"}]}}]}
          """, MediaType.APPLICATION_JSON));
      GeminiProperties properties = new GeminiProperties();
      properties.setApiKey("fixture-only");
      new GeminiRestClient(builder.build(), properties, mapper).generate(input);
      server.verify();
    }
    Path output = Path.of("build/ai-evaluation/requests.json");
    Files.createDirectories(output.getParent());
    Files.writeString(output, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fixtures));
  }
}
