package com.workora.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(servers = @Server(url = "/api/project", description = "API Gateway"))
public class ProjectServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjectServiceApplication.class, args);
    }
}
