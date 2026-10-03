package com.example.pdf_agent.Controller;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Messages;
import com.example.pdf_agent.Entities.User;
import com.example.pdf_agent.Services.ChatSessionService;
import com.example.pdf_agent.Services.MessageService;
import com.example.pdf_agent.Services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class Controller {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private MessageService messageService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running!");
    }

    @GetMapping("/getAllChats")
    public ResponseEntity<List<ChatSessions>> getAllChats(Authentication auth) {

        User user = userService.getUserByUsername(auth.getName());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<ChatSessions> chats = chatSessionService.getAllChats(user);
        return new ResponseEntity<>(chats, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            System.err.println("Error during registration: " + e.getMessage());
            return new ResponseEntity<>(new User(), HttpStatus.CONFLICT);
        }


    }

    @PostMapping("/login_user")
    public ResponseEntity<String> login(@RequestBody User user) {
        String response = userService.verify(user);
        if(response.equalsIgnoreCase("fail")) {
            return ResponseEntity.status(401).body("Authentication failed!");
        }
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    //request session id when starting a chat session, and then use that session id for subsequent messages in the same session
    @GetMapping("/generateSessionId")
    public String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }

    public record ChatRequest(@NotBlank String sessionId,
                              @NotBlank @Size(max = 4000) String message) {}

    @GetMapping("/Agent_response")
    public ResponseEntity<String> agentResponse(Authentication auth, @Valid @RequestBody ChatRequest userInput) {

        return ResponseEntity.ok(auth.getName() + " | Session ID: " + userInput.sessionId() + " | Message: " + userInput.message());
    }

    public record ChatRequest2(@NotBlank String sessionId,
                              @NotBlank String sessionName,
                              @NotBlank @Size(max = 4000) String message) {}

    @PostMapping("/saveSessionChats")
    public ResponseEntity<ChatSessions> saveSessionChats(Authentication auth, @Valid @RequestBody ChatRequest2 userInput) {
        ChatSessions sessionChats = new ChatSessions();

        sessionChats.setUser(userService.getUserByUsername(auth.getName()));
        sessionChats.setSessionID(userInput.sessionId());
        sessionChats.setSessionName(userInput.sessionName());

        Messages message = new Messages();
        message.setMessage(userInput.message());
        message.setChatSession(sessionChats);

        messageService.saveMessage(message);
        return ResponseEntity.ok(sessionChats);
    }
}
