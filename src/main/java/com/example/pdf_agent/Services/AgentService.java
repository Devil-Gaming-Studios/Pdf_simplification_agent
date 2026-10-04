package com.example.pdf_agent.Services;

import com.example.pdf_agent.Agents.Agent;
import com.example.pdf_agent.DB.PDF_Repo;
import com.example.pdf_agent.Entities.ChatSessions;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentService {
    private final InMemoryRunner runner;
    private final Runner simplificationRunner;


    @Autowired
    ChatSessionService chatSessionService;

    @Autowired
    SectionService sectionService;


    public AgentService(Agent agent) {
        try {
            this.runner = new InMemoryRunner(agent.Postprocessing_Agent());
            this.simplificationRunner = new Runner(agent.Preprocessing_Agent(), runner.appName(),
                    runner.artifactService(), runner.sessionService());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void createSession(String userId,String sessionId)
    {
        runner.sessionService().createSession(runner.appName(),userId,null,sessionId);
    }

    public void setPdfContent(String userId,String sessionId, String pdfContent)
    {
        // AgentService.setPdfContent: seed every key the instructions read
        ConcurrentHashMap<String, Object> state = new ConcurrentHashMap<>();
        state.put("pdf_text", pdfContent);
        state.put("session_id", sessionId);
        state.put("verification_report", "none yet");
        state.put("doc_summary", "");
        state.put("draft_answer", "");
        state.put("final_answer", "");
        runner.sessionService().createSession(runner.appName(), userId, state, sessionId).blockingGet();
        //runner.sessionService().createSession(runner.appName(), userId, state, sessionId).blockingGet();
    }
    public String startChat(ChatSessions chatSession) throws Exception {
        String sessionId = chatSession.getSessionID();
        String userId = chatSession.getUser().getId().toString();

        Content content = Content.fromParts(Part.fromText("Process this document."));
        simplificationRunner.runAsync(userId, sessionId, content).blockingSubscribe();

        var session = runner.sessionService()
                .getSession(runner.appName(), userId, sessionId, Optional.empty()).blockingGet();

        String json = (String) session.state().get("doc_sections");
        if (json != null) {
            try {
                sectionService.saveFromJson(chatSession, json);
            } catch (Exception e) {
                System.err.println("Section JSON parse failed: " + e.getMessage());   // retry once here if you like
            }
        }
        return (String) session.state().get("doc_simplification");
    }


    public String chat(ChatSessions session, String message) throws Exception
    {
        ensureSession(session);
        String sessionId = session.getSessionID();
        String userId = session.getUser().getId().toString();

        Content content = Content.fromParts(Part.fromText(message));
        StringBuilder reply = new StringBuilder();
        Flowable<Event> eventStream  = runner.runAsync(userId,sessionId,content);

        eventStream.blockingForEach(event -> {
            if (event.finalResponse()) {
                reply.append(event.stringifyContent());
            }
        });
        if(reply.isEmpty()) {
            throw new Exception("No response generated.");
        }

        return reply.toString();
    }


    public Flowable<Event> chatStream(ChatSessions s, String message) {
        ensureSession(s);
        return runner.runAsync(s.getUser().getId().toString(), s.getSessionID(),
                Content.fromParts(Part.fromText(message)));
    }

    private void ensureSession(ChatSessions s) {
        String userId = s.getUser().getId().toString();
        var existing = runner.sessionService()
                .getSession(runner.appName(), userId, s.getSessionID(), Optional.empty()).blockingGet();
        if (existing != null) return;                                   // still alive
        String pdf = chatSessionService.getPdfContent(s.getSessionID(), s.getUser());
        setPdfContent(userId, s.getSessionID(), pdf == null ? "" : pdf); // rebuild from MySQL
    }
}
