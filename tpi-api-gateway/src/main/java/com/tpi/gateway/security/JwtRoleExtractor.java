package com.tpi.gateway.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class JwtRoleExtractor {

    private JwtRoleExtractor() {
    }

    public static Set<String> extractRoles(Jwt jwt) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        addRoles(roles, jwt.getClaimAsStringList("roles"));

        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            addRoles(roles, realmAccessMap.get("roles"));
        }

        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
            resourceAccessMap.values().forEach(resource -> {
                if (resource instanceof Map<?, ?> resourceMap) {
                    addRoles(roles, resourceMap.get("roles"));
                }
            });
        }

        return roles;
    }

    public static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return extractRoles(jwt).stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private static void addRoles(Set<String> target, Object rolesValue) {
        if (rolesValue instanceof Iterable<?> iterable) {
            for (Object role : iterable) {
                if (role instanceof String roleName && !roleName.isBlank()) {
                    target.add(roleName);
                }
            }
        }
    }
}

