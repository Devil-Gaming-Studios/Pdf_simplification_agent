package com.example.pdf_agent.DB;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChunkRepo extends JpaRepository<Chunk, Integer> {
    List<Chunk> findByChatSessionOrderByPageAscChunkIndexAsc(ChatSessions session);
    void deleteByChatSession(ChatSessions session);

    List<Chunk> findByChatSession_SessionIDOrderByPageAscChunkIndexAsc(String sessionId);
}
