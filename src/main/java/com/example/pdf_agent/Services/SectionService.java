package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.ChunkRepo;
import com.example.pdf_agent.DB.SectionRepo;
import com.example.pdf_agent.Entities.Chunk;
import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Section;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SectionService {
    @Autowired
    SectionRepo repo;

    @Autowired
    ChunkRepo chunkRepo;

    private final JsonMapper mapper = JsonMapper.builder().build();

    // the real array starts with "[" followed by "{" (skips labels like "[simplify_translate] said:")
    private static final Pattern ARRAY_START = Pattern.compile("\\[\\s*\\{");

    @Transactional
    public List<Section> saveFromJson(ChatSessions session, String json) throws Exception {
        if (json == null) throw new IllegalArgumentException("Model output is empty");

        String cleaned = json.replaceAll("(?s)```(?:json)?", "");
        Matcher m = ARRAY_START.matcher(cleaned);
        if (!m.find()) throw new IllegalArgumentException("Model output has no JSON array of objects");
        int start = m.start();
        int end = cleaned.lastIndexOf(']');
        if (end <= start) throw new IllegalArgumentException("Model output has no closing ']'");
        cleaned = cleaned.substring(start, end + 1);

        JsonNode root = mapper.readTree(cleaned);
        if (!root.isArray() || root.isEmpty()) throw new IllegalArgumentException("No sections in model output");

        // original text comes from the stored page chunks, so the model never has to re-emit the PDF
        List<Chunk> chunks = chunkRepo.findByChatSession_SessionIDOrderByPageAscChunkIndexAsc(session.getSessionID());

        List<Section> out = new ArrayList<>();
        for (JsonNode n : root) {
            int pageStart = n.path("pageStart").asInt(1);
            int pageEnd = Math.max(pageStart, n.path("pageEnd").asInt(pageStart));

            String original = n.path("originalText").asText("");
            if (original.isBlank()) original = originalFor(chunks, pageStart, pageEnd);

            Section s = new Section();
            s.setChatSession(session);
            s.setTitle(n.path("title").asText("Untitled section"));
            s.setPageStart(pageStart);
            s.setPageEnd(pageEnd);
            s.setOriginalText(original);
            s.setSimplifiedText(n.path("simplifiedText").asText(""));
            s.setKeyTerms(n.path("keyTerms").toString());
            s.setWarnings(n.path("warnings").toString());
            out.add(s);
        }

        repo.deleteByChatSession(session);   // only after parsing succeeded, so a failed run keeps old data
        return repo.saveAll(out);
    }

    private String originalFor(List<Chunk> chunks, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (Chunk c : chunks) {
            int p = c.getPage();
            if (p >= from && p <= to) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(c.getText());
            }
        }
        return sb.toString();
    }
}