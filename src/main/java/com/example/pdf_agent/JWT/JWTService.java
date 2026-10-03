package com.example.pdf_agent.JWT;

import com.example.pdf_agent.Entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JWTService {
    private String secretKey;

    public JWTService() {
        try{
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            SecretKey key = keyGen.generateKey();
            this.secretKey = java.util.Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Map<String , Object> claims = new HashMap<>();
        System.out.println(claims);
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60*60*1000))
                .and()
                .signWith(getKey())
                .compact();
    }

    public String generateToken(String username) {
        Map<String , Object> claims = new HashMap<>();
        System.out.println(claims);
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60*60*1000))
                .and()
                .signWith(getKey())
                .compact();
    }

    public String extractUserName(String token) {
        String subject = getClaims(token).getSubject();
        return subject;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try{
            final String username = extractUserName(token);
            if(!username.equals(userDetails.getUsername()))
            {
                return false;
            }

            if(isTokenExpired(token))
            {
                return false;
            }

            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private boolean isTokenExpired(String token)
    {
        return extractExpiration(token).before(new Date(System.currentTimeMillis()));
    }

    private Date extractExpiration(String token)
    {
        return getClaims(token).getExpiration();
    }

    public Claims getClaims(String Token)
    {
        SecretKey key = (SecretKey) getKey();

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(Token)
                .getPayload();
    }

}
