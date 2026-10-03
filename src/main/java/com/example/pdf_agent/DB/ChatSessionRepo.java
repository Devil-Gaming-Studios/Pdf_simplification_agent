package com.example.pdf_agent.DB;

import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepo extends JpaRepository<ChatSessions, Long> {

    // Find all chat sessions belonging to a specific logged-in user
    List<ChatSessions> findByUser(User user);

    // Find a specific session by ID and User (ensures security check)
    ChatSessions findByIdAndUser(Integer id, User user);


}