package com.example.pdf_agent.DB;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepo extends JpaRepository<Section,Integer>
{
    List<Section> findByChatSessionOrderByPageStartAsc(ChatSessions s);
    void deleteByChatSession(ChatSessions s);
}