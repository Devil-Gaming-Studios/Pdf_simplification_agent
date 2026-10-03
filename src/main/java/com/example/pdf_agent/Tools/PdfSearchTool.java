package com.example.pdf_agent.Tools;

import com.example.pdf_agent.DB.ChunkRepo;
import com.example.pdf_agent.Entities.Chunk;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.ToolContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PdfSearchTool {
    private static final Set<String> STOP = Set.of("the","a","an","is","are","of","to","in","and","or","for","on","what","how","does","do","this","that","it");

    @Autowired
    ChunkRepo chunkRepo;

    @Schema(name = "search_pdf", description = "Finds the passages in the uploaded PDF most relevant to the query, with page numbers")
    public Map<String, Object> searchPdf(@Schema(name = "query") String query, ToolContext ctx) {
        String sessionId = ctx.sessionId();
        Set<String> words = Arrays.stream(query.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 2 && !STOP.contains(w)).collect(Collectors.toSet());

        List<Chunk> chunks = chunkRepo.findByChatSession_SessionIDOrderByPageAscChunkIndexAsc(sessionId);

        List<Map<String, Object>> top = chunks.stream()
                .map(c -> Map.entry(c, words.stream().filter(c.getText().toLowerCase()::contains).count()))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(4)
                .map(e -> Map.<String, Object>of("page", e.getKey().getPage(), "text", e.getKey().getText()))
                .toList();

        if (top.isEmpty()) return Map.of("passages", List.of(), "note", "No relevant passage found");
        return Map.of("passages", top);
    }
}
