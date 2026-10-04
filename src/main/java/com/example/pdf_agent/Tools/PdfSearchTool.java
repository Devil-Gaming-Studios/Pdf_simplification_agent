package com.example.pdf_agent.Tools;

import com.example.pdf_agent.DB.ChunkRepo;
import com.example.pdf_agent.Entities.Chunk;
import com.example.pdf_agent.Services.EmbeddingService;
import com.google.adk.tools.Annotations.Schema;

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

    @Autowired
    EmbeddingService embeddingService;

    @Schema(name = "search_pdf", description = "Finds the passages in the uploaded PDF most relevant to the query, with page numbers")
    public Map<String, Object> searchPdf(@Schema(name = "query") String query,
                                         @Schema(name = "sessionId") String sessionId) {
        Set<String> words = Arrays.stream(query.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 2 && !STOP.contains(w)).collect(Collectors.toSet());

        float[] q = embeddingService.embed(query);
        List<Chunk> chunks = chunkRepo.findByChatSession_SessionIDOrderByPageAscChunkIndexAsc(sessionId);
        if (chunks.isEmpty())
            return Map.of("passages", List.of(), "note", "Insufficient evidence in the document");

        record Scored(Chunk c, double score) {}
        List<Scored> ranked = chunks.stream()
                .filter(c -> c.getEmbedding() != null)
                .map(c -> {
                    double sem = EmbeddingService.cosine(q, EmbeddingService.fromStr(c.getEmbedding()));
                    double kw  = words.isEmpty() ? 0 : (double) words.stream().filter(c.getText().toLowerCase()::contains).count() / words.size();
                    return new Scored(c, 0.7 * sem + 0.3 * kw);   // hybrid score
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(10)
                .filter(s -> s.score() > 0.35)                    // below this = insufficient evidence
                .limit(4)
                .toList();

        if (ranked.isEmpty())
            return Map.of("passages", List.of(), "note", "Insufficient evidence in the document");

        return Map.of("passages", ranked.stream()
                .map(s -> Map.<String, Object>of(
                        "page", s.c().getPage(),
                        "score", Math.round(s.score() * 100) / 100.0,
                        "text", s.c().getText()))
                .toList());
    }
}