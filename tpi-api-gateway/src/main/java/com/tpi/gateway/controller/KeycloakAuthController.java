package com.tpi.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/login/oauth2")
public class KeycloakAuthController {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthController.class);

    private final WebClient webClient;
    private final String tokenEndpoint;
    private final String clientId;
    private final String redirectUri;

    public KeycloakAuthController(WebClient.Builder webClientBuilder,
                                   @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                                   @Value("${app.keycloak.client-id:tpi-backend-client}") String clientId,
                                   @Value("${app.keycloak.redirect-uri:http://localhost:8085/api/login/oauth2/code/keycloak}") String redirectUri) {
        this.webClient = webClientBuilder.build();
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
                .doOnNext(token -> log.info("Token recibido desde Keycloak: {}", token))
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("No se pudo intercambiar el code por token", ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .body("No se pudo intercambiar el code por token"));
                });
    }

    @GetMapping("/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return Mono.just(Map.of(
                "username", jwt.getClaimAsString("preferred_username"),
                "email", jwt.getClaimAsString("email"),
                "subject", jwt.getSubject(),
                "roles", roles != null ? roles : List.of(),
                "issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null
        ));
    }
}