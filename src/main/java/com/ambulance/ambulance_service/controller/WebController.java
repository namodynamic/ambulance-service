package com.ambulance.ambulance_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    /**
     * Serves the main application page
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * Handle all non-API routes to serve the SPA
     * This ensures that client-side routing works properly
     */
    @GetMapping({
            "/admin",
            "/admin/**",
            "/request",
            "/request/**",
            "/login",
            "/register"
    })
    public String spa() {
        return "forward:/index.html";
    }
}