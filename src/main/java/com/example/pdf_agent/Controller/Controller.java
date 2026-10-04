package com.example.pdf_agent.Controller;

import com.example.pdf_agent.DB.SectionRepo;
import com.example.pdf_agent.Entities.*;
import com.example.pdf_agent.Services.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import tools.jackson.databind.json.JsonMapper;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class Controller {

    @Autowired private UserService userService;
    @Autowired private ChatSessionService chatSessionService;
    @Autowired private MessageService messageService;
    @Autowired private AgentService agentService;
    @Autowired private OrchestratorService orchestratorService;
    @Autowired private PdfRenderService pdfRenderService;
    @Autowired private SectionRepo sectionRepo;
    @Autowired private ExecutorService streamExecutor;

    private final JsonMapper mapper = JsonMapper.builder().build();

    private void send(SseEmitter e, String name, String data) {
        try { e.send(SseEmitter.event().name(name).data(data)); } catch (Exception ignored) {}
    }

    private void step(SseEmitter e, Map<String, Object> m) {
        try { send(e, "step", mapper.writeValueAsString(m)); } catch (Exception ignored) {}
    }

    // ---------- DTOs ----------
    public record RegisterRequest(@NotBlank String username, @NotBlank String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) {}
    public record ChatRequest(@NotBlank String sessionId, @NotBlank @Size(max = 4000) String message) {}
    public record ChatSessionDto(String sessionId, String sessionName, Instant updatedAt) {}
    public record MessageDto(String role, String message, Instant createdAt) {}

    // ---------- helpers ----------
    private ChatSessions owned(Authentication auth, String sessionId) {
        User user = userService.getUserByUsername(auth.getName());
        return user == null ? null : chatSessionService.getSessionForUser(sessionId, user);
    }

    private void saveMessage(ChatSessions session, String role, String text) {
        Messages m = new Messages();
        m.setRole(role);
        m.setMessage(text);
        m.setChatSession(session);
        messageService.saveMessage(m);
    }

    // ---------- public ----------
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running!");
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req) {
        try {
            User u = new User();
            u.setUsername(req.username());
            u.setEmail(req.email());
            u.setPassword(req.password());          // hashed inside userService.register
            userService.register(u);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "registered"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already taken"));
        }
    }

    @PostMapping("/login_user")
    public ResponseEntity<String> login(@RequestBody User user) {
        String response = userService.verify(user);
        if (response.equalsIgnoreCase("fail"))
            return ResponseEntity.status(401).body("Authentication failed!");
        return ResponseEntity.ok(response);
    }

    // ---------- sessions ----------
    @GetMapping("/getAllChats")
    public ResponseEntity<List<ChatSessionDto>> getAllChats(Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(chatSessionService.getAllChats(user).stream()
                .map(c -> new ChatSessionDto(c.getSessionID(), c.getSessionName(), c.getUpdatedAt()))
                .toList());
    }

    @PostMapping("/generateSessionId")
    public ResponseEntity<Map<String, String>> generateSessionId(Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        if (user == null) return ResponseEntity.notFound().build();
        String sessionId = UUID.randomUUID().toString();

        ChatSessions session = new ChatSessions();
        session.setUser(user);
        session.setSessionName("New session");
        session.setSessionID(sessionId);
        chatSessionService.saveChatSession(session);

        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    @PatchMapping("/updateSessionName")
    public ResponseEntity<ChatSessionDto> updateSessionName(Authentication auth, @RequestBody Map<String, String> request) {
        ChatSessions session = owned(auth, request.get("sessionId"));
        if (session == null) return ResponseEntity.notFound().build();
        session.setSessionName(request.get("sessionName"));
        ChatSessions s = chatSessionService.saveChatSession(session);
        return ResponseEntity.ok(new ChatSessionDto(s.getSessionID(), s.getSessionName(), s.getUpdatedAt()));
    }

    @DeleteMapping("/deleteSession")
    public ResponseEntity<String> deleteSession(Authentication auth, @RequestParam String sessionId) {
        ChatSessions session = owned(auth, sessionId);
        if (session == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found");
        chatSessionService.deleteChatSession(sessionId, userService.getUserByUsername(auth.getName()));
        return ResponseEntity.ok("Session deleted successfully");
    }

    @GetMapping("/getSessionChats")
    public ResponseEntity<List<MessageDto>> getSessionChats(Authentication auth, @RequestParam String sessionId) {
        ChatSessions session = owned(auth, sessionId);
        if (session == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(messageService.getMessagesBySession(session).stream()
                .map(m -> new MessageDto(m.getRole(), m.getMessage(), m.getCreatedAt()))
                .toList());
    }

    // ---------- PDF + agents ----------
    @PostMapping("/uploadPDF")
    public ResponseEntity<String> uploadPDF(Authentication auth, @RequestParam("file") MultipartFile file,
                                            @RequestParam("sessionId") String sessionId) {
        User user = userService.getUserByUsername(auth.getName());
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        if (file.isEmpty() || !"application/pdf".equals(file.getContentType()))
            return ResponseEntity.badRequest().body("Upload a PDF file");

        ChatSessions session = chatSessionService.getSessionForUser(sessionId, user);
        if (session == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found for user");

        try {
            chatSessionService.savePdf(file, user, sessionId);      // tagged text + chunks
        } catch (Exception e) {
            System.err.println("Error during PDF upload: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading PDF");
        }

        String pdfContent = chatSessionService.getPdfContent(sessionId, user);
        agentService.setPdfContent(user.getId().toString(), sessionId, pdfContent);
        return ResponseEntity.ok(pdfContent.substring(0, Math.min(100, pdfContent.length())));
    }

    @PostMapping("/startChat")
    public ResponseEntity<String> startChatSession(Authentication auth, @RequestParam("sessionId") String sessionId) {
        ChatSessions session = owned(auth, sessionId);
        if (session == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found for user");
        try {
            String result = agentService.startChat(session);        // no createSession: it would wipe pdf_text
            saveMessage(session, "AGENT", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("startChat failed: " + e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Processing failed");
        }
    }

    @PostMapping("/agent_response")
    public ResponseEntity<?> agentResponse(Authentication auth, @Valid @RequestBody ChatRequest req) {
        ChatSessions session = owned(auth, req.sessionId());
        if (session == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Session not found for user"));

        saveMessage(session, "USER", req.message());                // saved even if the agent fails
        try {
            AgentResponse resp = orchestratorService.handleStructured(session, req.message());
            saveMessage(session, "AGENT", resp.answer());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            System.err.println("Error during agent response: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Agent failed"));
        }
    }

    @GetMapping("/download_pdf")
    public ResponseEntity<byte[]> downloadPdf(Authentication auth, @RequestParam String sessionId) throws IOException {
        ChatSessions session = owned(auth, sessionId);
        if (session == null) return ResponseEntity.notFound().build();

        List<Section> sections = sectionRepo.findByChatSessionOrderByPageStartAsc(session);
        if (sections.isEmpty()) return ResponseEntity.status(HttpStatus.CONFLICT).build();   // not processed yet

        byte[] pdf = pdfRenderService.render(session.getSessionName(), sections);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"simplified.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping(value = "/agent_response_stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agentResponseStream(Authentication auth, @Valid @RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(900_000L);   // 15 min while testing a local model
        ChatSessions session = owned(auth, req.sessionId());
        if (session == null) {
            send(emitter, "error", "Session not found");
            emitter.complete();
            return emitter;
        }
        saveMessage(session, "USER", req.message());

        java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>> task = new AtomicReference<>();
        emitter.onTimeout(() -> {
            send(emitter, "error", "Timed out");
            emitter.complete();
            var f = task.get();
            if (f != null) f.cancel(true);       // interrupts blockingSubscribe and stops the agent run
        });
        emitter.onError(t -> { var f = task.get(); if (f != null) f.cancel(true); });

        task.set(streamExecutor.submit(() -> {
            try {
                String[] draft = {""}, verified = {""};
                AtomicReference<Throwable> failure = new AtomicReference<>();
                send(emitter, "progress", "Processing your question...");

                agentService.chatStream(session, req.message()).blockingSubscribe(event -> {
                    String author = String.valueOf(event.author());

                    for (var fc : event.functionCalls())
                        step(emitter, Map.of("agent", author, "kind", "tool_call",
                                "name", fc.name().orElse("tool"), "args", fc.args().orElse(Map.of())));

                    for (var fr : event.functionResponses())
                        step(emitter, Map.of("agent", author, "kind", "tool_result",
                                "name", fr.name().orElse("tool"), "response", fr.response().orElse(Map.of())));

                    String text = event.stringifyContent();
                    if (event.finalResponse() && text != null && !text.isBlank()) {
                        step(emitter, Map.of("agent", author, "kind", "agent_output", "text", text));
                        if ("answer_verifier".equals(author)) verified[0] = text; else draft[0] = text;
                    }
                }, failure::set);

                if (Thread.currentThread().isInterrupted()) return;   // timed out or client left

                if (failure.get() != null) {
                    System.err.println("Stream failed: " + failure.get());
                    send(emitter, "error", "Agent failed");
                    emitter.complete();
                    return;
                }

                String answer = !verified[0].isBlank() ? verified[0] : draft[0];
                if (answer.isBlank()) answer = "No response generated.";
                saveMessage(session, "AGENT", answer);
                send(emitter, "answer", answer);
                emitter.complete();
            } catch (Throwable t) {
                System.err.println("Stream crashed: " + t);
                send(emitter, "error", "Agent failed");
                emitter.complete();
            }
        }));
        return emitter;
    }
    public record SectionDto(String title, int pageStart, int pageEnd,
                             String originalText, String simplifiedText, String warnings) {}

    @GetMapping("/sections")
    public ResponseEntity<List<SectionDto>> sections(Authentication auth, @RequestParam String sessionId) {
        ChatSessions session = owned(auth, sessionId);
        if (session == null) return ResponseEntity.notFound().build();

        List<Section> list = sectionRepo.findByChatSessionOrderByPageStartAsc(session);
        if (list.isEmpty()) return ResponseEntity.status(HttpStatus.CONFLICT).build();   // not processed yet

        return ResponseEntity.ok(list.stream()
                .map(s -> new SectionDto(s.getTitle(), s.getPageStart(), s.getPageEnd(),
                        s.getOriginalText(), s.getSimplifiedText(), s.getWarnings()))
                .toList());
    }
}