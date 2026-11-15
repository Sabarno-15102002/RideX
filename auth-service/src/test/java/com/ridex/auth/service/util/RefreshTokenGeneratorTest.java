package com.ridex.auth.service.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator generator = new RefreshTokenGenerator();

    @Test
    void generate_shouldReturnNonEmptyToken() {
        String token = generator.generate();

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generate_shouldReturnUrlSafeBase64WithoutPadding() {
        String token = generator.generate();

        assertFalse(token.contains("+"));
        assertFalse(token.contains("/"));
        assertFalse(token.contains("="));

        byte[] decoded = Base64.getUrlDecoder().decode(token);

        assertEquals(64, decoded.length);
    }

    @Test
    void generate_shouldReturnUniqueTokens() {
        String first = generator.generate();
        String second = generator.generate();

        assertNotEquals(first, second);
    }
}
