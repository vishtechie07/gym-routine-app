package com.gymtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home controller that handles the root URL and redirects to the main application.
 */
@Controller
public class HomeController {

    /**
     * Redirects the root URL to the main fitness tracker application.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/fitness-tracker.html";
    }
}
