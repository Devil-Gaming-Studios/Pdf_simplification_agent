package com.example.pdf_agent.Config;

import com.example.pdf_agent.DB.UserRepo;
import com.example.pdf_agent.Entities.User;
import com.example.pdf_agent.JWT.JWTFilter;
import com.example.pdf_agent.JWT.JWTService;
import com.example.pdf_agent.Services.MyUserDetailService;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String FRONTEND = "http://localhost:5500";

    @Autowired private MyUserDetailService myUserDetailsService;
    @Autowired private JWTFilter jwtFilter;
    @Autowired private JWTService jwtService;
    @Autowired private UserRepo userRepo;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults());          // uses the corsConfigurationSource bean below
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(request -> request
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                .requestMatchers("/register", "/login", "/login_user", "/health",
                        "/oauth2/**", "/login/oauth2/**").permitAll()
                .anyRequest().authenticated()
        );

        http.oauth2Login(oauth2 -> oauth2.successHandler((req, res, auth) -> {
            OAuth2User oauth2User = (OAuth2User) auth.getPrincipal();

            String login = oauth2User.getAttribute("login");   // GitHub only
            String username = login != null
                    ? "github_" + login
                    : "google_" + oauth2User.getAttribute("sub");

            String email = oauth2User.getAttribute("email");

            if (userRepo.findByUsername(username) == null) {
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setEmail(email != null ? email : username + "@oauth.com");
                newUser.setPassword("");   // OAuth users never log in with a password
                userRepo.save(newUser);
            }

            String token = jwtService.generateToken(username);
            res.sendRedirect(FRONTEND + "/#token=" + token);   // back to the frontend
        }));

        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(FRONTEND));
        c.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(myUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}