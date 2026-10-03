package com.example.pdf_agent.Agents;


import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.langchain4j.LangChain4j;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class Agent {

    private final BaseLlm ollama = LangChain4j.builder()
            .chatModel(OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("llama3.1")
                    .temperature(0.2)
                    .build())
            .modelName("llama3.1")
            .build();

    public LlmAgent understandingAgent() {
        return LlmAgent.builder()
                .name("doc_understanding")
                .model(ollama)
                .description("Summarizes the document")
                .instruction("Summarize the document: purpose, sections, key facts.\n\n{pdf_text}")
                .outputKey("doc_summary")
                .build();
    }

    public LlmAgent simplificationAgent() {
        return LlmAgent.builder()
                .name("simplify_translate")
                .model(ollama)
                .description("Simplifies or translates parts of the document")
                .instruction("Simplify or translate what the user asks, using {doc_summary} as context.")
                .build();
    }

    public BaseAgent initAgent() {
        return LlmAgent.builder()
                .name("coordinator")
                .model(ollama)
                .instruction("Route: summary -> doc_understanding; simplify/translate -> simplify_translate.")
                .subAgents(understandingAgent(), simplificationAgent())
                .build();
    }
}