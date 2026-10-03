package com.example.pdf_agent.DB;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Messages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Messages, Integer> {
    Messages getMessagesById(Integer id);

    List<Messages> findByChatSessionOrderByIdAsc(ChatSessions session);
    List<Messages> findByChatSessionId(Integer sessionId);
}
