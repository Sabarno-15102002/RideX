package com.ridex.auth.service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHashUtilTest {

    private final TokenHashUtil hashUtil = new TokenHashUtil();

    @Test
    void hash_shouldReturnSha256HexDigest() {
        String result = hashUtil.hash("test-token");

        assertNotNull(result);
        assertEquals(64, result.length());
        assertTrue(result.matches("[0-9a-f]{64}"));
    }

    @Test
    void hash_shouldReturnSameHashForSameInput() {
        String first = hashUtil.hash("same-token");
        String second = hashUtil.hash("same-token");

        assertEquals(first, second);
    }

    @Test
    void hash_shouldReturnDifferentHashesForDifferentInputs() {
        String first = hashUtil.hash("token-one");
        String second = hashUtil.hash("token-two");

        assertNotEquals(first, second);
    }

    @Test
    void hash_shouldReturnLowercaseHexadecimal() {
        String result = hashUtil.hash("test-token");

        assertTrue(result.matches("[0-9a-f]{64}"));
    }
}
