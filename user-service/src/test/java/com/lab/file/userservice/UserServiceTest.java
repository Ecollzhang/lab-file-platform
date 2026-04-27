package com.lab.file.userservice;

import com.lab.file.common.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    private final JwtUtil jwtUtil = new JwtUtil();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void testGenerateAndParseToken() {
        // 测试JWT工具类
        String token = jwtUtil.generateToken(1L, "testuser");
        assertNotNull(token);

        String username = jwtUtil.getUsernameFromToken(token);
        assertEquals("testuser", username);

        Long userId = jwtUtil.getUserIdFromToken(token);
        assertEquals(Long.valueOf(1L), userId);

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void testPasswordEncode() {
        // 测试密码加密
        String rawPassword = "testpassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        assertNotNull(encodedPassword);

        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }
}