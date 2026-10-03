package com.example.pdf_agent.Services;

import com.example.pdf_agent.Agents.Agent;
import com.example.pdf_agent.Entities.ChatSessions;
import com.google.adk.agents.BaseAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentService {
    private final InMemoryRunner runner;

    @Autowired
    Agent agent;

    public AgentService()
    {
        BaseAgent llmAgent = agent.initAgent();
        this.runner = new InMemoryRunner(llmAgent);
    }

    public void createSession(String userId,String sessionId)
    {
        runner.sessionService().createSession(runner.appName(),userId,null,sessionId);
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
