package com.example.pdf_agent.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PDF_Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    String fileName;
    String Content;

    @OneToOne
    @JoinColumn(name = "session_id")
    ChatSessions chatSession;
}
