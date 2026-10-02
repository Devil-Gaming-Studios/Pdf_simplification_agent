package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.UserRepo;
import com.example.pdf_agent.Entites.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    public User register(User user) {
        System.out.println("Registering: " + user.getUsername() + " | Password: " + user.getPassword());
        return userRepo.save(user);
    }

    public String verify(User user) {
        User foundUser = userRepo.findByUsername(user.getUsername());
        if (foundUser != null && foundUser.getPassword().equals(user.getPassword())) {
            System.out.println("Authentication successful for: " + user.getUsername());
            return "success";
        }
        System.out.println("Authentication failed for: " + user.getUsername());
        return "fail";
    }
}
