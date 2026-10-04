package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.MessageRepo;
import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepo messageRepo;

    public Messages saveMessage(Messages message) {
        return messageRepo.save(message);
    }

    public List<Messages> getMessagesBySession(ChatSessions session) {
        return messageRepo.findByChatSessionOrderByIdAsc(session);
    }
}