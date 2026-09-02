package com.humanizar.acolhimento.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig(mock(CorsConfigurationSource.class));

    @Test
    void jwtFilterShouldMapOnlyKeycloakRealmRoles() {
        Jwt jwt = jwt(Map.of(
                "realm_access", Map.of(
                        "roles", List.of(" ADMINISTRADOR ", "ROLE_COORDENADOR", "", 42, "ADMINISTRADOR")),
                "role", "ROLE_PROFISSIONAL",
                "roles", List.of("ROLE_RECEPCIONISTA")));

        var authentication = securityConfig.jwtFilter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting("authority")
                .filteredOn(authority -> ((String) authority).startsWith("ROLE_"))
                .containsExactly("ROLE_ADMINISTRADOR", "ROLE_COORDENADOR");
    }

    @Test
    void jwtFilterShouldIgnoreMissingOrMalformedRealmRoles() {
        Jwt missingRealmAccess = jwt(Map.of("role", "ROLE_ADMINISTRADOR"));
        Jwt malformedRealmAccess = jwt(Map.of("realm_access", Map.of("roles", "ADMINISTRADOR")));

        assertThat(securityConfig.jwtFilter().convert(missingRealmAccess).getAuthorities()).extracting("authority")
                .filteredOn(authority -> ((String) authority).startsWith("ROLE_"))
                .isEmpty();
        assertThat(securityConfig.jwtFilter().convert(malformedRealmAccess).getAuthorities()).extracting("authority")
                .filteredOn(authority -> ((String) authority).startsWith("ROLE_"))
                .isEmpty();
    }

    @Test
    void applicationYamlShouldRequireKeycloakConfigurationWithoutDefaults() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertThat(applicationYaml)
                .contains("jwk-set-uri: ${KEYCLOAK_ISSUER}")
                .contains("issuer-uri: ${JWT_ISSUER}")
                .contains("- ${JWT_AUDIENCE}")
                .contains("- RS256")
                .doesNotContain("AUTH_SERVER_URL")
                .doesNotContain("${KEYCLOAK_ISSUER:")
                .doesNotContain("${JWT_ISSUER:")
                .doesNotContain("${JWT_AUDIENCE:");
    }

    private Jwt jwt(Map<String, Object> claims) {
        Instant issuedAt = Instant.parse("2026-08-31T12:00:00Z");
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://auth.example/realms/humanizar")
                .subject("subject")
                .audience(List.of("humanizar-client"))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300));
        claims.forEach(jwtBuilder::claim);
        return jwtBuilder.build();
    }
}
