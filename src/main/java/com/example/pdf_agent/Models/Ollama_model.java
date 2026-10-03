package com.example.pdf_agent.Models;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.langchain4j.LangChain4j;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Ollama_model {
    private final BaseLlm ollama = LangChain4j.builder()
            .chatModel(OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("llama3.1")
                    .temperature(0.2)
                    .build())
            .modelName("llama3.1")
            .build();
}
