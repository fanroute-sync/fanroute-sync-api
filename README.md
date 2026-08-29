# FanRoute Sync API

부산 공연 관람 전후 여행 일정을 생성·편집하고 장소·공연 정보와 Fan Route 커뮤니티를 제공하는 Spring Boot API입니다.

## 기술 스택

- Java 21, Spring Boot, Gradle
- PostgreSQL, Redis
- Spring Data JPA, Spring Security, springdoc-openapi

## 로컬 실행

1. Java 21 JDK와 Docker를 설치합니다.
2. PostgreSQL·Redis 실행에 필요한 환경 변수를 설정합니다.
3. 로컬 의존 서비스를 시작합니다.

```bash
docker compose --env-file .env.local -f compose.local.yml up -d
```

4. 애플리케이션을 실행합니다.

```bash
bash ./gradlew bootRun
```

## 필수 환경 변수

| 구분 | 변수 |
| --- | --- |
| PostgreSQL | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` |
| 인증 | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, `JWT_SECRET` |
| 외부 API | `KOPIS_SERVICE_KEY`, `TOUR_API_KEY`, `GEMINI_API_KEY` |
| Docker Compose | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |

선택 환경 변수와 기본값은 [`application.yml`](src/main/resources/application.yml) 및
[`compose.local.yml`](compose.local.yml)을 확인합니다. 실제 키·비밀번호는 저장소에 커밋하지 않습니다.

## API 문서와 테스트

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- 테스트:

```bash
bash ./gradlew test
```

프로젝트 규칙과 화면·기획 기준은 [`docs/project/`](docs/project/)에서 확인합니다.
