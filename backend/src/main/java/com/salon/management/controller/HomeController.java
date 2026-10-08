package com.salon.management.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    // Health lives under /api/* so that "/" serves the bundled React UI.
    @GetMapping("/api/health")
    public String home() {
        return "Salon Management System backend is running.";
    }
}
