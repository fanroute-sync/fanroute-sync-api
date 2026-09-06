package com.fanroute.sync.domain.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.AiGenerationDeadLetter;

public interface AiGenerationDeadLetterRepository
    extends JpaRepository<AiGenerationDeadLetter, Long> {
}
