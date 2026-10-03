package com.example.pdf_agent.Agents;

import com.example.pdf_agent.Models.Ollama_model;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.tools.Annotations;
import com.google.adk.tools.FunctionTool;
import com.google.adk.tools.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PreProcessingAgent {

    private final BaseLlm MODEL;

    public PreProcessingAgent(Ollama_model ollamaModel) {
        this.MODEL = ollamaModel.getOllama();
    }

    public LlmAgent understandingAgent() {
        return LlmAgent.builder()
                .name("doc_understanding")
                .model(MODEL)
                .description("Extracts every important detail and summarizes the document")
                .instruction("You analyze {pdf_text}: write a short plain summary of its purpose, then extract every " +
                        "important feature as a structured list by page and section (headings, key facts, tables, " +
                        "numbers, percentages, dates, fees, conditions, exceptions, risk warnings, obligations, and key terms), " +
                        "keeping all figures and warnings exactly as written, adding no facts, and flagging any unclear or unreadable part. " +
                        "Previous verifier feedback (fix these problems if any): {verification_report}")
                .outputKey("doc_summary")
                .build();
    }

    public LlmAgent simplificationAgent() {
        return LlmAgent.builder()
                .name("simplify_translate")
                .model(MODEL)
                .description("Simplifies parts of the document")
                .instruction("You simplify {pdf_text} for an ordinary person with no finance or legal background, " +
                        "using {doc_summary} for context: rewrite each section in short, everyday sentences, replace jargon " +
                        "and legal terms with plain words (explain a needed term in brackets once), keep every number, " +
                        "percentage, date, fee, condition, exception and risk warning exactly as in the original and never " +
                        "drop a warning to sound simpler, keep the page and section reference with each part, do not add " +
                        "facts, opinions or buy/sell/hold advice, and if something is unclear say so instead of guessing. " +
                        "Previous verifier feedback (fix these problems if any): {verification_report}")
                .outputKey("doc_simplification")
                .build();
    }

    public static class ExitTool {
        @Annotations.Schema(name = "exit_loop", description = "Call only when doc_summary and doc_simplification are fully correct")
        public static Map<String, Object> exitLoop(ToolContext ctx) {
            ctx.actions().setEscalate(true);   // tells LoopAgent to stop
            return Map.of("status", "verified");
        }
    }

    public LlmAgent verificationAgent() {
        return LlmAgent.builder()
                .name("verifier")
                .model(MODEL)
                .description("Checks the summary and simplification against the original")
                .instruction("Compare {doc_summary} and {doc_simplification} with the original {pdf_text}. " +
                        "Check that every number, percentage, date, fee, condition, qualifier and risk warning is preserved exactly, " +
                        "and that nothing was invented. If everything is correct, call the exit_loop tool. " +
                        "Otherwise do NOT call it; list each problem briefly so the next pass can fix it.")
                .tools(FunctionTool.create(ExitTool.class, "exitLoop"))
                .outputKey("verification_report")
                .build();
    }

    // PreProcessingAgent: add formatter, and make the root a SequentialAgent(loop, formatter)
    public LlmAgent sectionFormatterAgent() {
        return LlmAgent.builder().name("section_formatter").model(MODEL)
                .instruction("Using {doc_summary} and {doc_simplification}, output ONLY a JSON array of objects "
                        + "{\"title\":string,\"pageStart\":int,\"pageEnd\":int,\"originalText\":string,\"simplifiedText\":string,"
                        + "\"keyTerms\":[string],\"warnings\":[string]}. Take pages from the [Page N] tags in {pdf_text}, copy numbers, "
                        + "dates, fees and risk warnings exactly, add no facts, no text outside the JSON.")
                .outputKey("doc_sections").build();
    }
    public BaseAgent PreProcessing_Agent() {
        LoopAgent loop = LoopAgent.builder().name("preprocessing_loop")
                .subAgents(understandingAgent(), simplificationAgent(), verificationAgent()).maxIterations(3).build();
        return SequentialAgent.builder().name("preprocessing").subAgents(loop, sectionFormatterAgent()).build();
    }

}