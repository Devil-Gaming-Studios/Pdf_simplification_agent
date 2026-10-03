package com.example.pdf_agent.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSessions chatSession;

    private String title;
    private int pageStart;
    private int pageEnd;
    @Column(columnDefinition = "LONGTEXT") private String originalText;
    @Column(columnDefinition = "LONGTEXT") private String simplifiedText;
    @Column(columnDefinition = "TEXT") private String keyTerms;   // JSON array as text
    @Column(columnDefinition = "TEXT") private String warnings;   // JSON array as text
}
