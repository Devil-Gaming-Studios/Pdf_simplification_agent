package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.SectionRepo;
import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Section;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

// SectionService
@Service
public class SectionService {
    @Autowired
    SectionRepo repo;
    private final JsonMapper mapper = JsonMapper.builder().build();   // tools.jackson.databind.json.JsonMapper

    @Transactional
    public List<Section> saveFromJson(ChatSessions session, String json) throws Exception {
        json = json.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", "").trim();   // strip markdown fences
        repo.deleteByChatSession(session);
        List<Section> out = new ArrayList<>();
        for (JsonNode n : mapper.readTree(json)) {
            Section s = new Section();
            s.setChatSession(session);
            s.setTitle(n.path("title").asText());
            s.setPageStart(n.path("pageStart").asInt());
            s.setPageEnd(n.path("pageEnd").asInt());
            s.setOriginalText(n.path("originalText").asText());
            s.setSimplifiedText(n.path("simplifiedText").asText());
            s.setKeyTerms(n.path("keyTerms").toString());
            s.setWarnings(n.path("warnings").toString());
            out.add(s);
        }
        return repo.saveAll(out);
    }
}
