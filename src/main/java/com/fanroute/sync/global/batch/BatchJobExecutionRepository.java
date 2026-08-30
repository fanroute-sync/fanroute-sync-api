package com.fanroute.sync.global.batch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Spring Batch가 자체 관리하는 {@code BATCH_JOB_EXECUTION} 등의 메타데이터 테이블을 조회 전용으로 읽습니다.
 * <p>
 * JPA Entity로 매핑하지 않고 {@link NamedParameterJdbcTemplate}으로 직접 조회합니다 — Batch가 이 테이블의
 * 스키마를 자체 초기화(`spring.batch.jdbc.initialize-schema`)하므로, JPA Entity로 매핑하면
 * {@code ddl-auto}가 같은 테이블을 이중으로 관리하려는 충돌이 생깁니다.
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class BatchJobExecutionRepository {

  private static final String SELECT_SQL = """
      select i.job_name, e.status, e.exit_code, e.start_time, e.end_time,
             coalesce(sum(s.read_count), 0) as read_count,
             coalesce(sum(s.write_count), 0) as write_count,
             coalesce(sum(s.read_skip_count), 0)
                 + coalesce(sum(s.process_skip_count), 0)
                 + coalesce(sum(s.write_skip_count), 0) as skip_count
      from batch_job_execution e
      join batch_job_instance i on i.job_instance_id = e.job_instance_id
      left join batch_step_execution s on s.job_execution_id = e.job_execution_id
      where i.job_name = :jobName
      group by e.job_execution_id, i.job_name, e.status, e.exit_code, e.start_time, e.end_time
      order by e.start_time desc nulls last, e.job_execution_id desc
      limit :limit offset :offset
      """;

  private static final String COUNT_SQL = """
      select count(*)
      from batch_job_execution e
      join batch_job_instance i on i.job_instance_id = e.job_instance_id
      where i.job_name = :jobName
      """;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /** 지정한 Job 이름의 실행 이력을 최근 실행순으로 DB에서 직접 페이징 조회합니다. */
  public Page<JobRunResult> findByJobName(String jobName, Pageable pageable) {
    Map<String, Object> params = Map.of(
        "jobName", jobName,
        "limit", pageable.getPageSize(),
        "offset", pageable.getOffset());

    List<JobRunResult> content = jdbcTemplate.query(SELECT_SQL, params, this::mapRow);
    long total = countByJobName(jobName);
    return new PageImpl<>(content, pageable, total);
  }

  private long countByJobName(String jobName) {
    Long total = jdbcTemplate.queryForObject(COUNT_SQL, Map.of("jobName", jobName), Long.class);
    return total == null ? 0 : total;
  }

  private JobRunResult mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new JobRunResult(
        rs.getString("job_name"),
        rs.getString("status"),
        rs.getString("exit_code"),
        rs.getLong("read_count"),
        rs.getLong("write_count"),
        rs.getLong("skip_count"),
        toLocalDateTime(rs.getTimestamp("start_time")),
        toLocalDateTime(rs.getTimestamp("end_time")));
  }

  private LocalDateTime toLocalDateTime(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }
}
