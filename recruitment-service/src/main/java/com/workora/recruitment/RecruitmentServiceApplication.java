package com.workora.recruitment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(servers = @Server(url = "/api/recruitment", description = "API Gateway"))
public class RecruitmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecruitmentServiceApplication.class, args);
    }
}
