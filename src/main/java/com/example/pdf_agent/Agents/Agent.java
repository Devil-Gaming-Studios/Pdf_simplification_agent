package com.example.pdf_agent.Agents;


import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import org.springframework.stereotype.Component;

@Component
public class Agent {
    private static BaseAgent ROOT_AGENT = initAgent();

    public static BaseAgent initAgent() {
        // Initialization logic for the root agent
        return SequentialAgent
                .builder()
                .name("Root Agent")
                .description("This is the root agent that orchestrates the workflow.")
                .subAgents()
                .build();
    }
}
