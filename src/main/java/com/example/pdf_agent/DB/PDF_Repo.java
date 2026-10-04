package com.example.pdf_agent.DB;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.PDF_Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PDF_Repo extends JpaRepository<PDF_Entity, Integer> {

    // Option A: pass the entity (rename so the name matches the signature)
    PDF_Entity findByChatSession(ChatSessions session);

    // Option B: pass the id instead
    // PDF_Entity findByChatSession_Id(Integer sessionId);
}
