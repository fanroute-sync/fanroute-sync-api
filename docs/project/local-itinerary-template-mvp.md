# 공연장 일정 템플릿 로컬 운영 가이드

공연장 ID 하나를 기준으로 TourAPI 후보를 수집하고, 로컬에서 운영자 또는 AI가 검수한 뒤
원격 서버에 장소와 일정 템플릿을 반영하는 MVP 절차다. 원격 서버에서는 후보 검색이나 AI를
자동 호출하지 않는다.

## 전제

- API 서버 주소는 `BASE_URL`로 지정한다. 예: `http://localhost:8080`
- 모든 `/admin/**`, `/internal/**` API는 관리자 JWT가 필요하다.
- 후보 조회는 공연장 좌표를 기준으로 반경 5,000m의 TourAPI를 실시간 호출한다.
- 현재 장소 카테고리는 `ACCOMMODATION`, `ATTRACTION`, `CULTURAL_FACILITY`, `SHOPPING`,
  `RESTAURANT`다. 카페는 `RESTAURANT` 안에서 `CAFE` 태그로 구분하고, 공원은
  `ATTRACTION` 안에서 `PARK` 태그로 구분한다.
- `template/`는 로컬 임시 작업 디렉터리다. 작업 시작 전에 기존 파일을 삭제하고, 운영 반영 후에도
  토큰과 후보 원본이 남지 않도록 삭제한다. 이 디렉터리의 파일은 Git에 커밋하지 않는다.

## 1. 공연장 ID와 후보 수집

공연장 목록 또는 상세 API로 대상 `VENUE_ID`를 확인한다.

```bash
export BASE_URL="http://localhost:8080"
export ADMIN_TOKEN="<관리자 JWT>"
export VENUE_ID="7"

rm -rf template
mkdir -p template
```

카테고리별로 후보를 조회한다. 응답은 저장되지 않으며, `contentId`와 `existingPlaceId`를
검수에 사용한다.

```bash
for category in ACCOMMODATION ATTRACTION CULTURAL_FACILITY SHOPPING RESTAURANT; do
  curl --fail-with-body -sS \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${BASE_URL}/api/v1/admin/places/nearby-candidates?venueId=${VENUE_ID}&category=${category}&radiusMeters=5000" \
    > "template/candidates-${category}.json"
done
```

`existingPlaceId`가 있으면 우리 DB에 이미 저장된 장소다. `null`이면 TourAPI에는 있지만
우리 DB에는 아직 없으므로, 템플릿에 넣기 전에 장소 반영 단계가 필요하다.

## 2. 로컬 검수와 `template.json` 작성

운영자 또는 로컬 AI가 후보의 이름·주소·카테고리·거리·영업 정보 등을 검토해 장소를 선택한다.
AI를 사용할 때도 후보 응답에 없는 장소를 새로 만들지 않고, 후보의 `contentId`를 기준으로
선택하도록 한다. 공연 전·후 시간대, 장소 간 이동 거리, 중복 카테고리 등을 확인한 뒤
`template/template.json`에 최종 선택 목록을 작성한다.

예시:

```json
{
  "venueId": 7,
  "name": "벡스코 공연 전후 추천 코스",
  "description": "공연 전 식사와 공연 후 관광을 포함한 기본 코스",
  "selectedCandidates": [
    {
      "contentId": "2868824",
      "name": "선택한 식당",
      "category": "RESTAURANT",
      "address": "부산광역시 ...",
      "latitude": 35.1699,
      "longitude": 129.1362,
      "telephone": "051-000-0000",
      "imageUrl": "https://...",
      "thumbnailUrl": "https://...",
      "copyrightType": "Type3",
      "sortOrder": 1,
      "defaultTime": "16:00",
      "defaultDurationMinutes": 60
    }
  ]
}
```

`selectedCandidates`는 템플릿 항목을 포함해 최대 5개로 구성한다. `sortOrder`는 1부터
시작하고, `defaultTime`과 `defaultDurationMinutes`는 비워도 된다. 해당 장소가 이미 DB에
있으면 원본 후보의 상세 값보다 서버의 `placeId`를 우선 사용한다.

## 3. 선택 장소를 원격 `places`에 반영

`template.json`의 `selectedCandidates`를 운영자가 최종 확인한 뒤, 장소 후보만 추려
`/api/v1/internal/places/import`에 전달한다. 이 API는 `contentId` 기준으로 장소를 upsert하고
저장된 `placeId`를 반환한다.

```http
POST /api/v1/internal/places/import
Authorization: Bearer <관리자 JWT>
Content-Type: application/json
```

```json
{
  "category": "RESTAURANT",
  "candidates": [
    {
      "contentId": "2868824",
      "name": "선택한 식당",
      "address": "부산광역시 ...",
      "latitude": 35.1699,
      "longitude": 129.1362,
      "telephone": "051-000-0000",
      "imageUrl": "https://...",
      "thumbnailUrl": "https://...",
      "copyrightType": "Type3"
    }
  ]
}
```

카테고리가 여러 개면 카테고리별로 요청한다. 응답의 `id`를 `template.json`의 각 후보와
매칭해 템플릿 반영용 `placeId` 목록을 만든다.

## 4. 대량 동기화가 필요한 경우

후보 선별 방식이 아니라 TourAPI 전체 장소를 먼저 동기화하려면 카테고리별 Job을 실행한다.
이 단계는 3단계의 선택 후보 반영을 대체할 수 있다.

```bash
for category in ACCOMMODATION ATTRACTION CULTURAL_FACILITY SHOPPING RESTAURANT; do
  curl --fail-with-body -sS -X POST \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${BASE_URL}/api/v1/admin/places/sync?category=${category}"
done
```

Job 완료 후 `nearby-candidates`를 다시 호출해 `existingPlaceId`를 확인한다. 동기화가
완료되지 않은 후보는 템플릿에 포함하지 않는다.

## 5. 일정 템플릿 반영

장소 반영이 끝난 뒤 `template.json`의 선택 순서와 반환된 `placeId`를 이용해 다음 API를
호출한다.

```http
POST /api/v1/internal/venue-itinerary-templates/import
Authorization: Bearer <관리자 JWT>
Content-Type: application/json
```

```json
{
  "venueId": 7,
  "name": "벡스코 공연 전후 추천 코스",
  "description": "공연 전 식사와 공연 후 관광을 포함한 기본 코스",
  "items": [
    {
      "placeId": 31,
      "sortOrder": 1,
      "defaultTime": "16:00",
      "defaultDurationMinutes": 60
    }
  ]
}
```

같은 공연장·템플릿 이름이 이미 있으면 기존 항목을 전체 교체한다. 따라서 반영 전에
템플릿 이름과 항목 순서를 최종 확인한다.

## 6. 반영 검증과 사용자 흐름 확인

```bash
curl --fail-with-body -sS \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${BASE_URL}/api/v1/venues/${VENUE_ID}/itinerary-templates"
```

일반 사용자는 공연장별 템플릿을 조회하고, 자신의 일정에 선택한 템플릿을 복사한다.
복사된 항목은 일반 일정 항목이므로 사용자가 시간·순서·삭제를 수정할 수 있다.

## MVP 완료 기준

- 공연장 ID 하나로 5km 반경의 카테고리별 TourAPI 후보를 수집할 수 있다.
- 로컬 임시 파일에서 운영자 또는 AI가 후보를 검수할 수 있다.
- 선택 장소를 원격 `places`에 중복 없이 반영할 수 있다.
- 저장된 `placeId`로 공연장 일정 템플릿을 원격 서버에 반영할 수 있다.
- 사용자가 템플릿을 자신의 일정으로 복사하고 수정할 수 있다.

이 절차는 관리자 UI나 자동화 스크립트가 없어도 Swagger·curl·간단한 로컬 스크립트만으로
MVP 운영이 가능한 형태다. 이후에는 `template.json` 생성·장소 import·템플릿 import를 하나의
로컬 CLI로 묶으면 된다.
