package com.wcy.shorturl.util;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Map;


public class JwtUtil {
    private static final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long jwt_ttl = 12 * 60 * 60 * 1000;

    public static String gettoken(Map<String, Object> claims)
    {
        return Jwts.builder()
                .signWith(secretKey)
                .addClaims(claims)
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + jwt_ttl))
                .compact();
    }
    public static Map<String, Object> parseJWT(String jwt)
    {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt)
                .getBody();
    }

}
