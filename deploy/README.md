# 배포 구성 (`fanroute-sync-api/deploy/`)

EC2 1대에서 `docker compose up -d`로 띄우는 배포 구성. 설계 배경은
`docs/superpowers/specs/2026-07-23-infra-design.md`, 실제 구축 절차는
`docs/superpowers/plans/2026-09-16-infra-deployment.md` 참고.

## 이전 `infra/` 구성과의 차이

- `backend`는 더 이상 이 저장소를 빌드 컨텍스트로 쓰지 않는다. GitHub Actions가
  빌드해 `ghcr.io/fanroute-sync/fanroute-sync-api`로 푸시한 이미지를 그대로 받아온다
  (`docker compose pull`). EC2에는 이 `deploy/` 세 파일(`docker-compose.yml`,
  `Caddyfile`, `.env`)만 있으면 되고, 소스 코드를 클론할 필요가 없다.
- `redis` 서비스가 추가됐다 — 리프레시 토큰 저장, AI 생성 비동기 워커(Redis
  Streams)가 실제로 Redis에 의존하므로 필수다.
- 백엔드 환경변수는 `application.yml`이 실제로 읽는 이름(`DB_URL` 등)으로
  맞췄다.

## `.env`

실제 값은 이 저장소에 커밋하지 않는다(`.gitignore`에 `deploy/.env` 추가 필요).
AWS SSM Parameter Store의 `/troadie/env-file`(SecureString)에 저장돼 있고,
EC2가 배포할 때마다 자기 IAM 역할로 그 값을 받아 `/opt/troadie/.env`를 새로
쓴다. 값을 바꾸려면 SSM Parameter Store만 갱신하면 된다 — EC2에 직접
SSH로 들어가 편집할 필요 없음(SSH 포트 자체가 막혀 있음).

## 로컬에서 전체 스택 확인하기

```bash
docker build -t ghcr.io/fanroute-sync/fanroute-sync-api:latest ..
cp .env.example .env   # 값 채우기
docker compose --env-file .env up -d
docker compose logs backend | grep Started
docker compose down -v
```
