package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.UserRepo;
import com.example.pdf_agent.Entities.User;
import com.example.pdf_agent.JWT.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private JWTService jwtService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public User register(User user) throws ResponseStatusException {
        if (userRepo.findByUsername(user.getUsername()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        System.out.println("Registering: " + user.getUsername() + " | Password: " + user.getPassword());
        return userRepo.save(user);
    }

    public String verify(User user) {
        try {
            System.out.println("Attempting to authenticate: " + user.getUsername());
            Authentication authentication = authenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );
            if (authentication.isAuthenticated()) {
                System.out.println("Authentication successful for: " + user.getUsername());
                return jwtService.generateToken(user);
            }
        } catch (Exception e) {
            System.out.println("Auth error: " + e.getMessage());
            e.printStackTrace();
        }
        return "fail";
    }

    public User getUserByUsername(String username)
    {
        User user  = userRepo.findByUsername(username);
        if(user != null)
        {
            return user;
        }
        else
        {
            System.out.println("User not found with username: " + username);
            return null;
        }
    }
}
