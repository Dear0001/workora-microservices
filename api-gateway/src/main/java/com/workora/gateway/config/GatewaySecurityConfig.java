package com.workora.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerAuthenticationEntryPoint authenticationEntryPoint,
            ServerAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource
    ) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(
                                "/actuator/health",
                                "/api/identity/api/health",
                                "/identify/actuator/health",
                                "/identify/swagger-ui.html",
                                "/identify/swagger-ui/**",
                                "/identify/v3/api-docs/**",
                                "/gateway/swagger-ui.html",
                                "/gateway/swagger-ui/**",
                                "/gateway/v3/api-docs/**",
                                "/organization/swagger-ui.html",
                                "/organization/swagger-ui/**",
                                "/organization/v3/api-docs/**",
                                "/project/swagger-ui.html",
                                "/project/swagger-ui/**",
                                "/project/v3/api-docs/**",
                                "/work/swagger-ui.html",
                                "/work/swagger-ui/**",
                                "/work/v3/api-docs/**",
                                "/bug/swagger-ui.html",
                                "/bug/swagger-ui/**",
                                "/bug/v3/api-docs/**",
                                "/recruitment/swagger-ui.html",
                                "/recruitment/swagger-ui/**",
                                "/recruitment/v3/api-docs/**",
                                "/notification/swagger-ui.html",
                                "/notification/swagger-ui/**",
                                "/notification/v3/api-docs/**",
                                "/payment/swagger-ui.html",
                                "/payment/swagger-ui/**",
                                "/payment/v3/api-docs/**"
                        ).permitAll()
                        .pathMatchers("/api/identity/api/v1/auth/**").permitAll()
                        .pathMatchers("/api/*/api/health").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                        new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter()))));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("ROLE_");
        authorities.setAuthoritiesClaimName("roles");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName("email");
        return converter;
    }

    @Bean
    ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, exception) -> {
            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(
                    "{\"success\":false,\"message\":\"Authentication required\"}".getBytes())));
        };
    }

    @Bean
    ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, exception) -> {
            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(
                    "{\"success\":false,\"message\":\"Access denied\"}".getBytes())));
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${gateway.cors.allowed-origins:http://localhost:3000}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
