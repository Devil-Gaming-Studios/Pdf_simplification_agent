package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.ChatSessionRepo;
import com.example.pdf_agent.DB.PDF_Repo;
import com.example.pdf_agent.Entities.ChatSessions;
import com.example.pdf_agent.Entities.PDF_Entity;
import com.example.pdf_agent.Entities.User;
import com.example.pdf_agent.Tools.PageText;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ChatSessionService {

    @Autowired private ChatSessionRepo chatSessionRepo;
    @Autowired private PDF_Repo pdfRepo;
    @Autowired private PdfExtractionService pdfExtractionService;
    @Autowired private ChunkService chunkService;

    public List<ChatSessions> getAllChats(User user) {
        return chatSessionRepo.findByUser(user);
    }

    // new session: rejects duplicates
    public ChatSessions createChatSession(ChatSessions chatSession) {
        if (chatSession == null || chatSession.getUser() == null)
            throw new IllegalArgumentException("Invalid chat session or user");
        if (getSessionForUser(chatSession.getSessionID(), chatSession.getUser()) != null)
            throw new IllegalStateException("Chat session already exists");
        return chatSessionRepo.save(chatSession);
    }

    // update an existing session (rename etc.)
    public ChatSessions saveChatSession(ChatSessions chatSession) {
        if (chatSession == null || chatSession.getUser() == null)
            throw new IllegalArgumentException("Invalid chat session or user");
        return chatSessionRepo.save(chatSession);
    }

    // ownership check: returns null if the session is not this user's
    public ChatSessions getSessionForUser(String sessionId, User user) {
        if (sessionId == null || user == null) return null;
        return chatSessionRepo.findBySessionIDAndUser(sessionId, user).orElse(null);
    }

    public void deleteChatSession(String sessionId, User user) {
        ChatSessions session = getSessionForUser(sessionId, user);
        if (session != null) chatSessionRepo.delete(session);
    }

    public String savePdf(MultipartFile file, User user, String sessionId) throws IOException, TesseractException {
        ChatSessions session = getSessionForUser(sessionId, user);
        if (session == null) throw new IllegalArgumentException("Session not found for user");

        List<PageText> pages = pdfExtractionService.extract(file.getBytes());
        String taggedText = pdfExtractionService.toTaggedText(pages);

        PDF_Entity pdfEntity = pdfRepo.findByChatSession(session);   // reuse the row on re-upload
        if (pdfEntity == null) pdfEntity = new PDF_Entity();
        pdfEntity.setFileName(file.getOriginalFilename());             // display only
        pdfEntity.setContent(taggedText);
        pdfEntity.setChatSession(session);
        pdfRepo.save(pdfEntity);

        chunkService.chunkAndSave(session, pages);                      // deletes old chunks first
        return taggedText;
    }

    public String getPdfContent(String sessionId, User user) {
        ChatSessions session = getSessionForUser(sessionId, user);
        if (session == null) return null;

        PDF_Entity pdfEntity = pdfRepo.findByChatSession(session);
        return pdfEntity == null ? null : pdfEntity.getContent();      // tagged text only, no file name
    }
}