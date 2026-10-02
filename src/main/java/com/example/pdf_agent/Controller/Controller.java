package com.example.pdf_agent.Controller;

import com.example.pdf_agent.Entites.User;
import com.example.pdf_agent.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @Autowired
    private UserService userService;

    @GetMapping("/health")
    public String health() {
        return "Service is running!";
    }

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.register(user);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        String response = userService.verify(user);
        if(response.equalsIgnoreCase("fail")) {
            return ResponseEntity.status(401).body("Authentication failed!");
        }
        return ResponseEntity.ok("Authentication successful!");
    }

    @PostMapping("/Agent_response")
    public String agentResponse(@RequestBody User response) {
        return "Agent response received!";
    }
}
