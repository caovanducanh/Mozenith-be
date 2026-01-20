package com.example.demologin.controller;

import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RequestMapping("api")
@RestController
public class CustomHealthController {

    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String DETAILS = "details";

    private final HealthEndpoint healthEndpoint;

    public CustomHealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    // @PublicEndpoint
    // @ApiResponse(message = "Health check response")
    @GetMapping("/health")
    public Object health() {
        HealthComponent healthComponent = healthEndpoint.health();
        Status status = healthComponent.getStatus();

        Map<String, Object> details = new LinkedHashMap<>();

        if (healthComponent instanceof CompositeHealth composite) {
            composite.getComponents().forEach((name, component) -> {
                if (component instanceof Health health) {
                    details.put(name, Map.of(
                            STATUS, health.getStatus().getCode(),
                            DETAILS, health.getDetails()
                    ));
                } else {
                    details.put(name, Map.of(STATUS, component.getStatus().getCode()));
                }
            });
        }
        if (healthComponent instanceof Health) {
            details.putAll(((Health) healthComponent).getDetails());
        }

        return Map.of(
                STATUS, status.getCode(),
                MESSAGE, status.equals(Status.UP) ? "Service is healthy" : "Service is unavailable",
                DETAILS, details
        );
    }
}
