package com.example.pdf_agent.Services;

import com.example.pdf_agent.Agents.Agent;
import com.example.pdf_agent.Entities.ChatSessions;
import com.google.adk.agents.BaseAgent;
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

    public AgentService(Agent agent)
    {
        BaseAgent llmAgent = agent.initAgent();
        this.runner = new InMemoryRunner(llmAgent);
        this.simplificationRunner = new Runner(agent.simplificationAgent(),runner.appName(),runner.artifactService(),runner.sessionService());
    }

    public void createSession(String userId,String sessionId)
    {
        runner.sessionService().createSession(runner.appName(),userId,null,sessionId);
    }

    public void setPdfContent(String userId,String sessionId, String pdfContent)
    {
        ConcurrentHashMap<String, Object> PDF_state = new ConcurrentHashMap<>();
        PDF_state.put("pdf_text", pdfContent);
        runner.sessionService().createSession(runner.appName(),userId,PDF_state,sessionId);
    }
    public String startChat(String userId,String sessionId) throws Exception
    {
        Content content = Content.fromParts(Part.fromText("Transcribe the document in a simple language for a normal person to understand."));
        StringBuilder reply = new StringBuilder();
        try {
            simplificationRunner.runAsync(userId, sessionId, content).blockingSubscribe();   // outputKey writes doc_summary to state

            var session = runner.sessionService()
                    .getSession(runner.appName(), userId, sessionId, Optional.empty()).blockingGet();

            return (String) session.state().get("doc_summary");
        }
        catch( Exception e)
        {
            throw e;
        }
    }

    public String chat(ChatSessions session, String message) throws Exception
    {
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
}
