package com.tpi.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtRoleExtractorTest {

    @Test
    void extractRolesSupportsFlatRolesClaim() {
        Jwt jwt = jwt(Map.of("roles", List.of("USER")));

        assertEquals(Set.of("USER"), JwtRoleExtractor.extractRoles(jwt));
        assertEquals(Set.of("ROLE_USER"), authorityNames(jwt));
    }

    @Test
    void extractRolesSupportsStandardKeycloakRealmAccessClaim() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("USER", "ADMIN"))));

        assertEquals(Set.of("USER", "ADMIN"), JwtRoleExtractor.extractRoles(jwt));
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"), authorityNames(jwt));
    }

    @Test
    void extractRolesMergesClaimsWithoutDuplicatingAuthorities() {
        Jwt jwt = jwt(Map.of(
                "roles", List.of("USER"),
                "realm_access", Map.of("roles", List.of("ADMIN", "USER")),
                "resource_access", Map.of("tpi-postman", Map.of("roles", List.of("USER")))
        ));

        assertEquals(Set.of("USER", "ADMIN"), JwtRoleExtractor.extractRoles(jwt));
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"), authorityNames(jwt));
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                claims
        );
    }

    private Set<String> authorityNames(Jwt jwt) {
        return JwtRoleExtractor.extractAuthorities(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
    }
}

