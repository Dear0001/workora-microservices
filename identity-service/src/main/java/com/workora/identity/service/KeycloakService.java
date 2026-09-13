package com.workora.identity.service;

import com.workora.identity.dto.LoginRequest;
import com.workora.identity.dto.RefreshTokenRequest;
import com.workora.identity.dto.RegisterRequest;
import com.workora.identity.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Map;

@Service
public class KeycloakService {

    private final RestClient client;
    private final String realm;
    private final String clientId;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakService(
            RestClient.Builder builder,
            @Value("${keycloak.base-url}") String baseUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.admin-username}") String adminUsername,
            @Value("${keycloak.admin-password}") String adminPassword
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.realm = realm;
        this.clientId = clientId;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public void register(RegisterRequest request) {
        String adminToken = adminToken();
        var created = client.post()
                .uri("/admin/realms/{realm}/users", realm)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "username", request.email().trim().toLowerCase(),
                        "email", request.email().trim().toLowerCase(),
                        "firstName", request.firstName() == null ? "" : request.firstName(),
                        "lastName", request.lastName() == null ? "" : request.lastName(),
                        "enabled", true,
                        "emailVerified", false,
                        "realmRoles", new String[]{"USER"},
                        "credentials", new Object[]{
                                Map.of("type", "password", "value", request.password(), "temporary", false)
                        }
                ))
                .retrieve()
                .toBodilessEntity();

        try {
            sendRequiredAction(request.email(), "VERIFY_EMAIL", adminToken);
        } catch (RestClientResponseException ex) {
            URI location = created.getHeaders().getLocation();
            if (location != null) {
                String userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
                client.delete()
                        .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                        .headers(headers -> headers.setBearerAuth(adminToken))
                        .retrieve()
                        .toBodilessEntity();
            }
            throw new AppException(
                    "Keycloak could not send the verification email. Configure SMTP in Realm settings > Email."
            );
        }
    }

    public Map<String, Object> login(LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", request.email());
        form.add("password", request.password());
        form.add("scope", "openid profile email");
        return tokenRequest(form);
    }

    public Map<String, Object> refresh(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", request.refreshToken());
        return tokenRequest(form);
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);
        client.post()
                .uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    public void requestPasswordReset(String email) {
        sendRequiredAction(email, "UPDATE_PASSWORD", adminToken());
    }

    private void sendRequiredAction(String email, String action, String adminToken) {
        Map<String, Object>[] users = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("username", email.trim().toLowerCase())
                        .build(realm))
                .headers(headers -> headers.setBearerAuth(adminToken))
                .retrieve()
                .body(Map[].class);
        if (users == null || users.length == 0) {
            throw new IllegalStateException("Keycloak user was not found for email action");
        }

        client.put()
                .uri("/admin/realms/{realm}/users/{id}/execute-actions-email", realm, users[0].get("id"))
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new String[]{action})
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> tokenRequest(MultiValueMap<String, String> form) {
        return client.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
    }

    private String adminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);
        Map<String, Object> response = client.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        return (String) response.get("access_token");
    }
}
