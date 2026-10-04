package com.example.pdf_agent.DB;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepo extends JpaRepository<ChatSessions, Integer> {

    List<ChatSessions> findByUser(User user);

    // matches the sessionID field (the UUID), scoped to the owner
    Optional<ChatSessions> findBySessionIDAndUser(String sessionID, User user);
}