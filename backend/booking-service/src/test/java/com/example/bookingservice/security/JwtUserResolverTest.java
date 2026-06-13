package com.example.bookingservice.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUserResolverTest {

    private final JwtUserResolver resolver = new JwtUserResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_returnsSubjectFromJwt() {
        UUID expected = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(expected.toString());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        UUID result = resolver.getCurrentUserId();

        assertThat(result).isEqualTo(expected);
    }
}