# 자동 검증 기록

검증 환경: Java 21, Gradle wrapper, macOS + Colima Docker, PostgreSQL 17 / Redis 7.4
Testcontainers. 실제 애플리케이션의 DB나 Redis 데이터는 사용하지 않았다.

## 일정 도메인 최종 회귀

```sh
DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
AI_EVALUATION_EXPORT=true \
bash gradlew test --tests 'com.fanroute.sync.domain.schedule.*' --console=plain
```

108개 통과, 실패 0, 제외 0. 포함 범위:

- 한국 시간 도착·출발·자정 경계, 일반 일정과 생성 항목의 겹침, 미정 시각·체류시간.
- 후보 태그·좌표·숙소 맥락, 장소 ID 제한, 강도별 개수, 정상 종료 응답.
- 추론 설정과 성공·실패·중복 완료 시 단계별 로그.
- 실제 DB에서 모델 호출 중 커밋된 수동 일정의 충돌 거부, 경계 시각 허용, 사용량 차감·해제.
- DB 저장 실패 롤백, Redis 장애·작업 회수·재시도·알림 outbox.
- 여행 스타일 한국어 라벨 저장·재조회 및 비교 실험 요청 내보내기.

## 전체 회귀 및 기준 브랜치 대조

전체 `bash gradlew test`는 372개 중 360개 통과, 11개 실패, 1개 제외였다.
제외된 테스트는 명시적으로 `AI_EVALUATION_EXPORT=true`를 지정해야 하는 요청 내보내기 테스트다.
이후 중복 완료 로그 수정과 테스트 2개를 추가하고, 위 일정 도메인 108개를 다시 통과했다.

아래 실패 11개는 변경 전 `origin/develop`의 `45ae9eb`에서도 동일 명령·환경으로 재현했다.
요청 범위 밖의 코드와 설정은 변경하지 않았다.

| 테스트 | 실패 수 | 최초 원인 |
| --- | ---: | --- |
| `PlaceAdminControllerTest` | 8 | Spring Batch `Job` 빈 3개 중 컨트롤러 생성자 주입 대상이 모호함 |
| `BatchJobExecutionRepositoryTest` | 3 | 전체 컨텍스트에서 `${REDIS_PORT}` 환경 설정이 제공되지 않음 |

AI 복구·재시도·저장 실패 통합 테스트는 기존에 `AiPlaceCandidateRanker` 등록이 누락되어 있었다.
이번 변경과 관련된 통합 검증을 실행하기 위해 세 테스트의 `@Import`만 보완했다.

## 실험 도구

```sh
python3 -m unittest discover -s tools/ai-evaluation -p 'test_*.py'
git diff --check
```

평가 도구 테스트 7개 통과. 시간 경계·초 단위 기존 시각·미정 체류시간·개수·후보 ID·
비교 기준 요청·집계 분모를 확인했다. HTTP 200이어도 파싱할 수 없는 결과는 제약별 준수율의
분모에서 제외하며, 전체 제약 준수에는 실패로 남긴다.

baseline 재구성 요청 20개는 이전 Java 클라이언트가 내보낸 요청과 JSON 객체로 대조해 모두
일치했다. MINIMAL 요청 20개도 현재 Java 클라이언트 내보내기와 대조해 모두 일치했다.
실제 모델 호출은 [실험 기록](README.md)과 원시 응답에서 확인한다.

## 배포 설정 검사

`docker-compose --env-file deploy/.env.example -f deploy/docker-compose.yml config --quiet`는
기존 `FIREBASE_ENABLED: ${FIREBASE_ENABLED:false}`의 잘못된 Compose 보간 구문으로 실패했다.
동일 구문이 `origin/develop`에도 존재함을 확인했으며 이번 AI 변경에서는 수정하지 않았다.
새 `GEMINI_MODEL`·`GEMINI_THINKING_LEVEL` 변수는 Compose의 `:-` 기본값 구문을 사용한다.
