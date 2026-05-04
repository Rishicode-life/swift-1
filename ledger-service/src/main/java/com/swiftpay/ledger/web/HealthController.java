package com.swiftpay.ledger.web;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    public HealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        var health = healthEndpoint.health();
        var body = Map.of("status", health.getStatus().getCode());
        if (health.getStatus() == Status.DOWN) {
            return ResponseEntity.status(503).body(body);
        }
        return ResponseEntity.ok(body);
    }
}
