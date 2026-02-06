package com.codebuddy.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Object health() {
        return Collections.singletonMap("status", "UP");
    }
}
