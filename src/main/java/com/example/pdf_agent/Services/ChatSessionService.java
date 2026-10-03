package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.ChatSessionRepo;
import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepo chatSessionRepo;

    public List<ChatSessions> getAllChats(User user) {
        List<ChatSessions> chatList =  chatSessionRepo.findByUser(user);
        if(chatList.isEmpty()) {
            System.out.println("No chat sessions found for user: " + user.getUsername());
        } else {
            System.out.println("Retrieved " + chatList.size() + " chat sessions for user: " + user.getUsername());
        }
        return chatList;
    }

    public ChatSessions saveChatSession(ChatSessions chatSession) {
        if(chatSession == null || chatSession.getUser() == null) {
            System.out.println("Invalid chat session or user. Cannot save.");
            return new ChatSessions(); // Return an empty ChatSessions object to indicate failure
        }

        if(chatSessionRepo.findByIdAndUser(chatSession.getId() , chatSession.getUser()) != null) {
            System.out.println("Chat session with ID: " + chatSession.getId() + " already exists for user: " + chatSession.getUser().getUsername());
            return new ChatSessions(); // Return an empty ChatSessions object to indicate failure
        }

        ChatSessions savedSession = chatSessionRepo.save(chatSession);
        if(savedSession == null) {
            System.out.println("Failed to save chat session for user: " + chatSession.getUser().getUsername());
            return new ChatSessions(); // Return an empty ChatSessions object to indicate failure
        } else {
            System.out.println("Saved chat session with ID: " + savedSession.getId() + " for user: " + savedSession.getUser().getUsername());
            return savedSession;
        }
    }
}
