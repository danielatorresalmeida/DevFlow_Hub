package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.SystemRole;
import com.devflowhub.backend.config.JwtProperties;
import com.devflowhub.backend.entity.Collaborator;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void issueCreatesSignedTokenWithExpectedClaims() {
        SecretKey secretKey = new SecretKeySpec(
                "test-secret-with-more-than-32-bytes!"
                        .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        JwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        JwtProperties properties = new JwtProperties(
                "unused-in-this-unit-test",
                "https://devflow-hub.test",
                Duration.ofMinutes(15)
        );

        JwtTokenService service = new JwtTokenService(encoder, properties);

        Collaborator collaborator = new Collaborator();
        collaborator.setId(7L);
        collaborator.setName("Ana Silva");
        collaborator.setEmail("ana@example.com");
        collaborator.setRole("Developer");
        collaborator.setSystemRole(SystemRole.ADMIN);
        collaborator.setActive(true);

        JwtTokenService.IssuedToken issuedToken = service.issue(collaborator);

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer("https://devflow-hub.test")
        );

        Jwt jwt = decoder.decode(issuedToken.accessToken());

        assertThat(issuedToken.expiresInSeconds()).isEqualTo(900);
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getIssuer().toString()).isEqualTo("https://devflow-hub.test");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ana@example.com");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Ana Silva");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("Developer");
        assertThat(jwt.getClaimAsString("system_role")).isEqualTo("ADMIN");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }
}
