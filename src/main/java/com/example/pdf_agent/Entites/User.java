package com.example.pdf_agent.Entites;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    private Integer id;

    private String username;
    private String email;
    private String password;

    @OneToMany
    private List<ChatSessions> sessions;
}
