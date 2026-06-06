package com.medifamba.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${medifamba.jwt.secret}")
    private String secret;

    @Value("${medifamba.jwt.expiration}")
    private long expiration;

    @Value("${medifamba.jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UserDetails user) {
        return build(user, expiration);
    }

    public String generateRefreshToken(UserDetails user) {
        return build(user, refreshExpiration);
    }

    private String build(UserDetails user, long exp) {
        return Jwts.builder()
                .claims(new HashMap<>())
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + exp))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(Jwts.parser().verifyWith(getKey())
                .build().parseSignedClaims(token).getPayload());
    }

    public boolean isTokenValid(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername())
                && !extractClaim(token, Claims::getExpiration).before(new Date());
    }
}