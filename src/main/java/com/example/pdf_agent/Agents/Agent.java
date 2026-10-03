package com.example.pdf_agent.Agents;


import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.langchain4j.LangChain4j;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class Agent {
    @Autowired
    PreProcessingAgent preProcessingAgent;

    @Autowired
    PostProcessingAgent postProcessingAgent;

    public BaseAgent Preprocessing_Agent()
    {
        return preProcessingAgent.PreProcessing_Agent();
    }

    public BaseAgent Postprocessing_Agent(){
        return postProcessingAgent.PostProcessing_Agent();
    }
}