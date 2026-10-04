package com.example.pdf_agent.Services;

import com.example.pdf_agent.Agents.Agent;
import com.example.pdf_agent.DB.ChunkRepo;
import com.example.pdf_agent.Entities.AgentResponse;
import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Chunk;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class OrchestratorService {
    private final Runner intentRunner;
    @Autowired
    AgentService agentService;

    @Autowired
    ChatSessionService chatSessionService;

    @Autowired
    GuardrailService guardrails;

    public OrchestratorService(Agent agent, AgentService as) {
        this.intentRunner = new InMemoryRunner(agent.intentAgent());
    }

    private String classify(String msg) {
        String sid = UUID.randomUUID().toString();   // throwaway session each time
        intentRunner.sessionService().createSession(intentRunner.appName(), "classifier", null, sid).blockingGet();
        StringBuilder out = new StringBuilder();
        intentRunner.runAsync("classifier", sid, Content.fromParts(Part.fromText(msg)))
                .blockingForEach(e -> { if (e.finalResponse())
                    out.append(e.stringifyContent()); });
        return out.toString().trim().toLowerCase().replaceAll("[^a-z_]", "");
    }

    public String handle(ChatSessions s, String msg) throws Exception {
        return switch (classify(msg)) {
            case "document_processing", "simplification" -> agentService.startChat(s);
            case "document_question" -> agentService.chat(s, msg);
            case "general_financial_question" -> agentService.chat(s, msg);   // swap to knowledge-base agent in item 5
            case "clarification" -> "Could you tell me which part of the document or topic you mean?";
            default -> "I can't give personal buy/sell/hold advice, but I can explain how the product, its risks and fees work.";
        };
    }
    @Autowired
    ChunkRepo chunkRepo;

    public AgentResponse handleStructured(ChatSessions s, String msg) throws Exception {
        String traceId = UUID.randomUUID().toString();
        // OrchestratorService.handleStructured: replace "String answer = handle(s, msg);" with this block
        String pdfText = chatSessionService.getPdfContent(s.getSessionID(), s.getUser());   // your existing method, returns the tagged text
        List<String> warnings = new ArrayList<>(guardrails.ocrWarnings(pdfText));

        if (guardrails.isAdviceRequest(msg))
            return new AgentResponse("I can't give personal buy, sell or hold advice, but I can explain how this product, its risks and fees work.",
                    List.of(), "DECLINED", warnings, traceId);

        String answer = handle(s, msg);
        List<String> bad = guardrails.unsupportedNumbers(answer, pdfText);
        if (!bad.isEmpty()) {                                    // regenerate once
            answer = handle(s, msg + "\n\nYour previous answer contained numbers not in the document: " + bad
                    + ". Answer again using only numbers that appear in the passages.");
            bad = guardrails.unsupportedNumbers(answer, pdfText);
        }
        String status = null;
        if (!bad.isEmpty()) {
            status = "FAILED_VERIFICATION";
            warnings.add("Some numbers could not be matched to the document: " + bad);
        }
// later, keep your existing sources/status code, but only set status if it is still null


        // pages cited like "Page 3" or "[Page 3]"
        Set<Integer> pages = new TreeSet<>();
        Matcher m = Pattern.compile("(?i)page\\s*(\\d+)").matcher(answer);
        while (m.find()) pages.add(Integer.parseInt(m.group(1)));

        List<Chunk> chunks = chunkRepo.findByChatSession_SessionIDOrderByPageAscChunkIndexAsc(s.getSessionID());
        List<AgentResponse.Source> sources = chunks.stream()
                .filter(c -> pages.contains(c.getPage()))
                .collect(Collectors.toMap(Chunk::getPage, c -> c, (a, b) -> a, TreeMap::new)).values().stream()
                .map(c -> new AgentResponse.Source(c.getPage(), c.getText().substring(0, Math.min(200, c.getText().length()))))
                .toList();


        if (answer.toLowerCase().contains("not found in the document") || answer.toLowerCase().contains("insufficient evidence")) {
            status = "INSUFFICIENT_EVIDENCE";
            warnings.add("The document does not contain enough evidence to answer.");
        } else if (sources.isEmpty()) {
            status = "UNVERIFIED";
            warnings.add("No page citation found in the answer.");
        } else status = "VERIFIED";

        return new AgentResponse(answer, sources, status, warnings, traceId);
    }
}
