# AI 일정 생성 비교 실험

동일 합성 시나리오를 고정 seed로 섞어 순차 호출한 결과다. 운영 서비스의 before/after 실측이 아니다.

| 조건 | 요청 | HTTP 성공 | 전체 제약 준수 | 평균 ms | p50 ms | p95 ms | 총 토큰 |
|---|---:|---:|---:|---:|---:|---:|---:|
| baseline | 60 | 60 | 39 | 1398.77 | 1393.72 | 1611.04 | 38152 |
| minimal | 60 | 60 | 60 | 1382.33 | 1337.34 | 1642.49 | 66580 |
| low | 60 | 60 | 60 | 1411.82 | 1416.18 | 1644.5 | 66793 |

지연은 HTTP 왕복과 응답 본문 수신 시간이며 앱 큐·DB·화면 반영은 제외한다. 실패 요청도 지연 집계에 포함한다.
제약 준수는 시간 범위·겹침·개수·체류시간·제목·후보 ID·중복·정상 종료를 모두 통과한 요청 수다.
이번 요약에는 총 180개 실행 행과 20개 시나리오가 포함된다. 독립적인 실제 사용자 만족도 결과로 해석할 수 없다.
태그 적합률·직선 경로·CUSTOM 비율은 각각 유효한 장소 선택·거리 계산·일정 항목을 분모로 한 보조 지표다. 실제 도로 이동시간·영업시간·사용자 만족도는 검증하지 않았다.

## 보조 지표 및 제약 준수율

| 조건 | 태그 적합 | 태그 분모 | 직선 경로 평균 km | 거리 분모 | CUSTOM 비율 | CUSTOM 분모 | 제약 분모 |
|---|---:|---:|---:|---:|---:|---:|---:|
| baseline | 0.4857 | 175 | 6.5092 | 57 | 0.0 | 175 | 60 |
| minimal | 0.2897 | 145 | 1.0357 | 57 | 0.0523 | 153 | 60 |
| low | 0.2867 | 150 | 1.0275 | 57 | 0.0385 | 156 | 60 |

제약별 준수율은 HTTP 200이면서 transport_or_parse_error가 없는 평가 가능 응답을 분모로 계산한다.

| 조건 | 제약 | 준수율 |
|---|---|---:|
| baseline | item_count | 1.0 |
| baseline | travel_window | 0.8 |
| baseline | existing_overlap | 0.9 |
| baseline | generated_overlap | 1.0 |
| baseline | duration | 1.0 |
| baseline | title | 1.0 |
| baseline | unknown_place | 1.0 |
| baseline | duplicate_place | 1.0 |
| baseline | time_format | 1.0 |
| baseline | invalid_item | 1.0 |
| baseline | empty_or_invalid_items | 0.95 |
| baseline | finish_reason | 1.0 |
| minimal | item_count | 1.0 |
| minimal | travel_window | 1.0 |
| minimal | existing_overlap | 1.0 |
| minimal | generated_overlap | 1.0 |
| minimal | duration | 1.0 |
| minimal | title | 1.0 |
| minimal | unknown_place | 1.0 |
| minimal | duplicate_place | 1.0 |
| minimal | time_format | 1.0 |
| minimal | invalid_item | 1.0 |
| minimal | empty_or_invalid_items | 1.0 |
| minimal | finish_reason | 1.0 |
| low | item_count | 1.0 |
| low | travel_window | 1.0 |
| low | existing_overlap | 1.0 |
| low | generated_overlap | 1.0 |
| low | duration | 1.0 |
| low | title | 1.0 |
| low | unknown_place | 1.0 |
| low | duplicate_place | 1.0 |
| low | time_format | 1.0 |
| low | invalid_item | 1.0 |
| low | empty_or_invalid_items | 1.0 |
| low | finish_reason | 1.0 |

태그 적합률과 직선 경로는 보조 지표다. 실제 도로 이동시간·영업시간·사용자 만족도는 검증하지 않았다.

## 위반 유형 (한 요청에 여러 유형 가능)

| 조건 | 유형 | 건수 |
|---|---|---:|
| baseline | empty_or_invalid_items | 3 |
| baseline | existing_overlap | 6 |
| baseline | travel_window | 12 |
