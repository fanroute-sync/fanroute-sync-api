-- Hibernate는 WHERE 조건이 있는 부분 유니크 인덱스를 엔티티 애노테이션으로 만들 수 없습니다.
-- 회차의 유일성은 공연 전체가 아니라 "같은 날짜·시각"을 기준으로 하고, 소프트 삭제된 행은 제외해야
-- 삭제된 회차의 시각을 재사용할 수 있습니다. 테이블 생성 후 이 제약만 보완합니다.
-- 애플리케이션 재시작마다 실행되므로 DROP/CREATE 모두 IF (NOT) EXISTS로 멱등성을 보장합니다.
DROP INDEX IF EXISTS uk_concert_schedules_concert_id_round;

CREATE UNIQUE INDEX IF NOT EXISTS uk_concert_schedules_concert_id_date_time
    ON concert_schedules (concert_id, performance_date, performance_time)
    WHERE deleted_at IS NULL;
