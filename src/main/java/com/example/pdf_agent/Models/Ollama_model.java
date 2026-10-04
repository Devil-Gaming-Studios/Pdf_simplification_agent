package com.example.pdf_agent.Models;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.langchain4j.LangChain4j;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Getter
public class Ollama_model {
    private final BaseLlm ollama = LangChain4j.builder()
            .chatModel(OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("llama3.1:8b")
                    .temperature(0.2)
                    .timeout(Duration.ofMinutes(10))
                    .maxRetries(0)
                    .numCtx(8192)
                    .build())
            .streamingChatModel(OllamaStreamingChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("llama3.1:8b")
                    .temperature(0.2)
                    .timeout(Duration.ofMinutes(10))
                    .numCtx(8192)
                    .build())
            .modelName("llama3.1:8b")
            .build();
}