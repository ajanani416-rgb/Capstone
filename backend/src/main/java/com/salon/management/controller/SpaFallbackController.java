package com.salon.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Forwards client-side routes to the bundled React app so a single host can
 * serve UI + API (e.g. http://localhost:8080/login). API paths and real
 * static assets are untouched — only extensionless non-API paths forward. */
@Controller
public class SpaFallbackController {

    @GetMapping(value = {
            "/login", "/register", "/services", "/403",
            "/customer", "/customer/**",
            "/admin", "/admin/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
