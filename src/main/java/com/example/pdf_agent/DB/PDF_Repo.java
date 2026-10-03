package com.example.pdf_agent.DB;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.PDF_Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PDF_Repo extends JpaRepository<PDF_Entity, Integer> {
    PDF_Entity findByChatSessionId(ChatSessions session);
}
