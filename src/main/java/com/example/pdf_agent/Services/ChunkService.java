package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.ChunkRepo;
import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Chunk;
import com.example.pdf_agent.Tools.PageText;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkService {
    @Autowired
    ChunkRepo chunkRepo;

    @Autowired
    EmbeddingService embeddingService;

    @Transactional
    public List<Chunk> chunkAndSave(ChatSessions session, List<PageText> pages) {
        chunkRepo.deleteByChatSession(session);          // re-upload replaces old chunks
        List<Chunk> out = new ArrayList<>();
        int idx = 0;
        for (PageText p : pages) {
            for (String piece : split(p.text(), 1000, 150)) {
                Chunk c = new Chunk();
                c.setChatSession(session);
                c.setPage(p.page());
                c.setChunkIndex(idx++);
                c.setText(piece);
                c.setEmbedding(EmbeddingService.toStr(embeddingService.embed(piece)));
                out.add(c);
            }
        }
        return chunkRepo.saveAll(out);
    }

    // splits on paragraph/sentence boundaries when possible, with overlap
    static List<String> split(String text, int size, int overlap) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + size);
            if (end < text.length()) {
                int cut = Math.max(text.lastIndexOf("\n\n", end), text.lastIndexOf(". ", end));
                if (cut > start + size / 2) end = cut + 1;
            }
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) parts.add(piece);
            if (end >= text.length()) break;
            start = Math.max(end - overlap, start + 1);
        }
        return parts;
    }
}
