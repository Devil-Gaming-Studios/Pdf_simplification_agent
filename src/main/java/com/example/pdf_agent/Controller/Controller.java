package com.example.pdf_agent.Controller;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Messages;
import com.example.pdf_agent.Entities.User;
import com.example.pdf_agent.Services.AgentService;
import com.example.pdf_agent.Services.ChatSessionService;
import com.example.pdf_agent.Services.MessageService;
import com.example.pdf_agent.Services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.jcip.annotations.NotThreadSafe;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.Response.ok;

@RestController
public class Controller {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private MessageService messageService;

    @Autowired
    AgentService agentService;

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
        if (response.equalsIgnoreCase("fail")) {
            return ResponseEntity.status(401).body("Authentication failed!");
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //request session id when starting a chat session, and then use that session id for subsequent messages in the same session
    @GetMapping("/generateSessionId")
    public ResponseEntity<Map<String, String>> generateSessionId(Authentication auth) {
        String sessionId = java.util.UUID.randomUUID().toString();
        HashMap<String, String> response = new HashMap<>();
        response.put("SessionID", sessionId);
        User user = userService.getUserByUsername(auth.getName());
        ChatSessions session = new ChatSessions();
        session.setUser(user);
        session.setSessionName("New session");
        session.setSessionID(sessionId);

        //agentService.createSession(user.getId().toString(), sessionId);
        chatSessionService.saveChatSession(session);

        return ResponseEntity.ok(response);
    }

    public record ChatRequest(@NotBlank String sessionId,
                              @NotBlank @Size(max = 4000) String message) {
    }

    @GetMapping("/Agent_response")
    public ResponseEntity<Map<String, String>> agentResponse(Authentication auth, @Valid @RequestBody ChatRequest userInput) {
        User user = userService.getUserByUsername(auth.getName());
        ChatSessions session = chatSessionService.getSessionForUser(userInput.sessionId, user);
        try {
            if (session == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found for user");
            }
        } catch (ResponseStatusException e) {
            System.err.println("Error: " + e.getMessage());
            return ResponseEntity.ok(Map.of("error", e.getReason()));
        }
        try {
            String response = agentService.chat(session, userInput.message());
            Map<String, String> map = new HashMap<>();
            map.put(session.getSessionID(), response);
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            System.err.println("Error during agent response: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }

    }

    public record ChatRequest2(@NotBlank String sessionId,
                               @NotBlank String sessionName,
                               @NotBlank @Size(max = 4000) String message) {
    }

    @PostMapping("/saveSessionChats")
    public ResponseEntity<ChatSessions> saveSessionChats(Authentication auth, @Valid @RequestBody ChatRequest2 userInput) {
        ChatSessions sessionChats = chatSessionService.getSessionForUser(userInput.sessionId(), userService.getUserByUsername(auth.getName()));
        if (sessionChats == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        sessionChats.setSessionName(userInput.sessionName());

        Messages message = new Messages();
        message.setMessage(userInput.message());
        message.setRole("User");
        message.setChatSession(sessionChats);

        messageService.saveMessage(message);
        return ResponseEntity.ok(sessionChats);
    }

    @DeleteMapping("/deleteSession")
    public ResponseEntity<String> deleteSession(Authentication auth, @RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        User user = userService.getUserByUsername(auth.getName());
        ChatSessions session = chatSessionService.getSessionForUser(sessionId, user);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found for user");
        }
        chatSessionService.deleteChatSession(sessionId, user);
        return ResponseEntity.ok("Session deleted successfully");
    }

    @GetMapping("/getSessionChats")
    public ResponseEntity<List<Messages>> getSessionChats(Authentication auth, @RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        User user = userService.getUserByUsername(auth.getName());
        ChatSessions session = chatSessionService.getSessionForUser(sessionId, user);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        List<Messages> messages = messageService.getMessagesBySession(session);
        return ResponseEntity.ok(messages);
    }

    @PatchMapping("/updateSessionName")
    public ResponseEntity<ChatSessions> updateSessionName(Authentication auth, @RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String newSessionName = request.get("sessionName");
        User user = userService.getUserByUsername(auth.getName());
        ChatSessions session = chatSessionService.getSessionForUser(sessionId, user);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        session.setSessionName(newSessionName);
        ChatSessions updatedSession = chatSessionService.saveChatSession(session);
        return ResponseEntity.ok(updatedSession);
    }

    @PostMapping("/uploadPDF")
    public ResponseEntity<String> uploadPDF(Authentication auth, @RequestParam("file") MultipartFile file, @RequestParam("sessionId") String sessionId) {
        User user = userService.getUserByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        try {
            chatSessionService.savePdf(file, user, sessionId);
        } catch (IOException e) {
            System.err.println("Error during PDF upload: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading PDF: " + e.getMessage());
        }

        ChatSessions session = chatSessionService.getSessionForUser(sessionId, user);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found for user");
        }

        String pdfContent = chatSessionService.getPdfContent(sessionId, user);
        agentService.setPdfContent(user.getId().toString(), sessionId, pdfContent);
        return ResponseEntity.ok(pdfContent.substring(0, 100));
    }

    @GetMapping("/startChat")
    public ResponseEntity<String> startChatSession(Authentication auth, @RequestParam("sessionId") String sessionId) {
        User user = userService.getUserByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        agentService.createSession(user.getId().toString(), sessionId);
        try {
            String result = agentService.startChat(user.getId().toString(),sessionId);
            return new ResponseEntity<>(result,HttpStatus.OK);
        }catch(Exception e)
        {
            return new ResponseEntity<>(e.toString(),HttpStatus.BAD_REQUEST);
        }
    }
}
