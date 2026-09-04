package com.workora.bug;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(servers = @Server(url = "/api/bug", description = "API Gateway"))
public class BugServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BugServiceApplication.class, args);
    }
}
