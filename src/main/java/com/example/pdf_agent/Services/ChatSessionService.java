package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.ChatSessionRepo;
import com.example.pdf_agent.DB.PDF_Repo;
import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.PDF_Entity;
import com.example.pdf_agent.Entities.User;
import com.example.pdf_agent.Tools.OCR_Tool;
import com.example.pdf_agent.Tools.Text_Extractor;
import com.nimbusds.openid.connect.sdk.claims.SessionID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepo chatSessionRepo;

    @Autowired
    private PDF_Repo pdfRepo;

    @Autowired
    private OCR_Tool ocrTool;

    @Autowired
    private Text_Extractor textExtractor;

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

        if(chatSessionRepo.findByIdAndUser(chatSession.getSessionID() , chatSession.getUser()) != null) {
            System.out.println("Chat session with ID: " + chatSession.getSessionID() + " already exists for user: " + chatSession.getUser().getUsername());
            return new ChatSessions(); // Return an empty ChatSessions object to indicate failure
        }

        ChatSessions savedSession = chatSessionRepo.save(chatSession);
        if(savedSession == null) {
            System.out.println("Failed to save chat session for user: " + chatSession.getUser().getUsername());
            return new ChatSessions(); // Return an empty ChatSessions object to indicate failure
        } else {
            System.out.println("Saved chat session with ID: " + savedSession.getSessionID() + " for user: " + savedSession.getUser().getUsername());
            return savedSession;
        }
    }

    public ChatSessions getSessionForUser(String sessionId, User user)
    {
        List<ChatSessions> list = getAllChats(user);
        return list.stream().filter(chatSession -> {return chatSession.getSessionID().equals(sessionId);}).findFirst().orElse(null);
    }

    public void deleteChatSession(String sessionId, User user) {
        ChatSessions session = getSessionForUser(sessionId, user);
        if(session != null) {
            chatSessionRepo.delete(session);
            System.out.println("Deleted chat session with ID: " + sessionId + " for user: " + user.getUsername());
        } else {
            System.out.println("No chat session found with ID: " + sessionId + " for user: " + user.getUsername());
        }
    }

    public String savePdf(MultipartFile file,User user, String sessionId) throws IOException
    {
            String fileName = file.getOriginalFilename();
            byte[] content = file.getBytes();
            String OCRContent = ocrTool.ocr_tool(content);
            String textContent = textExtractor.text_extractor(content);

            String finalContent = OCRContent.length() > textContent.length() ? OCRContent : textContent;
            PDF_Entity pdfEntity = new PDF_Entity();
            pdfEntity.setFileName(fileName);
            pdfEntity.setContent(finalContent);
            pdfEntity.setChatSession(chatSessionRepo.findByIdAndUser(sessionId, user));

            pdfRepo.save(pdfEntity);

            return finalContent;

    }

    public String getPdfContent(String sessionId, User user) {
        ChatSessions session = getSessionForUser(sessionId, user);
        if (session == null) {
            System.out.println("No chat session found with ID: " + sessionId + " for user: " + user.getUsername());
            return null;
        }

        PDF_Entity pdfEntity = pdfRepo.findByChatSessionId(session);
        if (pdfEntity == null) {
            System.out.println("No PDF found for chat session with ID: " + sessionId);
            return null;
        }
        return pdfEntity.getContent();
    }
}
