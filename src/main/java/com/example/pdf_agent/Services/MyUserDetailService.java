package com.example.pdf_agent.Services;

import com.example.pdf_agent.DB.UserRepo;
import com.example.pdf_agent.Entites.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class MyUserDetailService implements UserDetailsService {

    @Autowired
    private UserRepo repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repo.findByUsername(username);
        System.out.println("Looking for: " + username);
        System.out.println("Found: " + (user != null ? user.getUsername() : "NULL"));
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        System.out.println("Password hash: " + user.getPassword());
        return new UserPrincipal(user);
    }
}
