package com.workora.work;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(servers = @Server(url = "/api/work", description = "API Gateway"))
public class WorkServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkServiceApplication.class, args);
    }
}
