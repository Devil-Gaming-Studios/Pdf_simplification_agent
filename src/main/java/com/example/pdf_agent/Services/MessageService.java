package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.MessageRepo;
import com.example.pdf_agent.Entities.Messages;
import org.apache.logging.log4j.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService
{
    @Autowired
    private MessageRepo messageRepo;

    public List<Messages> fetchMessageBySessionId(Integer sessionId) {
        return messageRepo.findByChatSessionId(sessionId);
    }
    public Messages saveMessage(Messages message) {
        return messageRepo.save(message);
    }
}
