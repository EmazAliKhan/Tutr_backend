package com.tutr.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";
    private static final String OTHER_SECRET = "9999999999999999999999999999999999999999";

    private JwtUtil newJwtUtil(String secret, long expMs) {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", secret);
        ReflectionTestUtils.setField(jwt, "expirationMs", expMs);
        return jwt;
    }

    @Test
    void generateAndParse_roundTrip() {
        JwtUtil jwt = newJwtUtil(SECRET, 60_000);
        String token = jwt.generateToken(42L, "a@b.com", "STUDENT");

        assertThat(jwt.isTokenValid(token)).isTrue();
        assertThat(jwt.extractEmail(token)).isEqualTo("a@b.com");
        assertThat(jwt.extractRole(token)).isEqualTo("STUDENT");
        assertThat(jwt.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void expiredToken_isInvalid() throws InterruptedException {
        JwtUtil jwt = newJwtUtil(SECRET, 1); // 1 ms validity
        String token = jwt.generateToken(1L, "a@b.com", "STUDENT");

        Thread.sleep(50); // let it expire

        assertThat(jwt.isTokenValid(token)).isFalse();
    }

    @Test
    void garbageToken_isInvalid() {
        JwtUtil jwt = newJwtUtil(SECRET, 60_000);
        assertThat(jwt.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    void tokenSignedWithDifferentKey_isInvalid() {
        JwtUtil a = newJwtUtil(SECRET, 60_000);
        JwtUtil b = newJwtUtil(OTHER_SECRET, 60_000);

        String token = a.generateToken(1L, "a@b.com", "STUDENT");

        assertThat(b.isTokenValid(token)).isFalse();
    }
}