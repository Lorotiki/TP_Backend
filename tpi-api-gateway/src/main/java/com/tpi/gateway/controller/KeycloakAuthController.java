package com.tpi.gateway.controller;

import com.tpi.gateway.security.JwtRoleExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/login/oauth2")
public class KeycloakAuthController {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthController.class);

    private final WebClient webClient;
    private final ReactiveJwtDecoder jwtDecoder;
    private final String tokenEndpoint;
    private final String clientId;
    private final String redirectUri;

    public KeycloakAuthController(WebClient.Builder webClientBuilder,
                                   ReactiveJwtDecoder jwtDecoder,
                                   @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                                   @Value("${app.keycloak.client-id:tpi-backend-client}") String clientId,
                                   @Value("${app.keycloak.redirect-uri:http://localhost:8085/api/login/oauth2/code/keycloak}") String redirectUri) {
        this.webClient = webClientBuilder.build();
        this.jwtDecoder = jwtDecoder;
        this.tokenEndpoint = issuerUri + "/protocol/openid-connect/token";
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    @GetMapping("/code/keycloak")
    public Mono<ResponseEntity<String>> intercambiarCode(@RequestParam String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);

        return webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(token -> log.info("Token recibido desde Keycloak"))
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("No se pudo intercambiar el code por token", ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .body("No se pudo intercambiar el code por token"));
                });
    }

    @GetMapping("/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        List<String> roles = new ArrayList<>(JwtRoleExtractor.extractRoles(jwt));
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authenticated", authentication.isAuthenticated());
        response.put("username", jwt.getClaimAsString("preferred_username"));
        response.put("email", jwt.getClaimAsString("email"));
        response.put("subject", jwt.getSubject());
        response.put("roles", roles);
        response.put("authorities", authorities);
        response.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        response.put("tokenValid", true);
        response.put("tokenType", jwt.getHeaders().getOrDefault("typ", "JWT"));
        response.put("issuedAt", jwt.getIssuedAt() != null ? jwt.getIssuedAt().toString() : null);
        response.put("expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : null);
        response.put("canAccessAdmin", authorities.contains("ROLE_ADMIN"));
        response.put("canAccessUser", authorities.contains("ROLE_USER") || authorities.contains("ROLE_ADMIN"));

        return Mono.just(response);
    }

    @GetMapping("/debug-token")
    public Mono<ResponseEntity<Map<String, Object>>> debugToken(@RequestParam String token) {
        return jwtDecoder.decode(token)
                .map(jwt -> {
            List<String> roles = new ArrayList<>(JwtRoleExtractor.extractRoles(jwt));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("valid", true);
            response.put("username", jwt.getClaimAsString("preferred_username"));
            response.put("email", jwt.getClaimAsString("email"));
            response.put("subject", jwt.getSubject());
            response.put("roles", roles);
            response.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
            response.put("issuedAt", jwt.getIssuedAt() != null ? jwt.getIssuedAt().toString() : null);
            response.put("expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : null);
            response.put("canAccessAdmin", roles.contains("ADMIN"));
            response.put("canAccessUser", roles.contains("USER") || roles.contains("ADMIN"));

            return ResponseEntity.ok(response);
        })
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "valid", false,
                    "error", ex.getMessage()
            ))));
    }
}
