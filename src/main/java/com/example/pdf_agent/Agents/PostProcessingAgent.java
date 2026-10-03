package com.example.pdf_agent.Agents;

import com.example.pdf_agent.Models.Ollama_model;
import com.example.pdf_agent.Tools.PdfSearchTool;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.tools.FunctionTool;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PostProcessingAgent {
    private final BaseLlm MODEL;
    private final PdfSearchTool pdfSearchTool;

    public PostProcessingAgent(Ollama_model ollamaModel, PdfSearchTool pdfSearchTool) {
        this.MODEL = ollamaModel.getOllama();
        this.pdfSearchTool = pdfSearchTool;
    }

    public LlmAgent qaAgent() {
        return LlmAgent.builder()
                .name("qa_agent")
                .model(MODEL)
                .description("Answers questions about the uploaded document")
                .instruction("Answer the user's question using ONLY passages from search_pdf, with {doc_summary} for context. "
                        + "Cite the page number from each passage for every factual claim, keep numbers, dates, fees and warnings exactly as written, "
                        + "give no buy/sell/hold advice, and if the document lacks the answer say 'Not found in the document'.")
                .tools(FunctionTool.create(pdfSearchTool, "searchPdf"))
                .outputKey("draft_answer")
                .build();
    }

    public LlmAgent verifierAgent() {
        return LlmAgent.builder()
                .name("answer_verifier")
                .model(MODEL)
                .description("Checks the draft answer against the document")
                .instruction("Check {draft_answer} against passages from search_pdf. Remove or flag any unsupported claim, "
                        + "fix any changed number, date, fee or missing qualifier, and output only the final corrected answer with its page citations.")
                .tools(FunctionTool.create(pdfSearchTool, "searchPdf"))
                .outputKey("final_answer")
                .build();
    }

    public BaseAgent PostProcessing_Agent() {
        return SequentialAgent.builder()
                .name("postprocessing_qna")
                .subAgents(qaAgent(), verifierAgent())
                .build();
    }
}