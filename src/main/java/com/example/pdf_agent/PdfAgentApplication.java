package com.example.pdf_agent;

import com.example.pdf_agent.DB.UserRepo;
import com.example.pdf_agent.Entites.ChatSessions;
import com.example.pdf_agent.Entites.User;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PdfAgentApplication {

    @Autowired
    private UserRepo userRepository;

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        SpringApplication.run(PdfAgentApplication.class, args);
    }


}