package com.fanroute.sync.domain.user.service;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "신나는", "친절한", "열정적인", "용감한", "즐거운",
            "따뜻한", "빛나는", "활기찬", "설레는", "행복한");

    private static final List<String> NOUNS = List.of(
            "고래", "여우", "토끼", "판다", "수달",
            "돌고래", "갈매기", "별빛", "여행자", "파도");

    private static final int RANDOM_NUMBER_BOUND = 10_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        String adjective = randomElement(ADJECTIVES);
        String noun = randomElement(NOUNS);
        int number = secureRandom.nextInt(RANDOM_NUMBER_BOUND);
        return "%s%s%04d".formatted(adjective, noun, number);
    }

    private String randomElement(List<String> values) {
        return values.get(secureRandom.nextInt(values.size()));
    }
}
