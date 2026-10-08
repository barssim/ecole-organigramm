package com.ecole.sales.security;

import com.ecole.sales.config.AppProperties;
import com.ecole.sales.user.SalesUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.*;

@Service
public class JwtService {
    private final AppProperties properties;
    private Key key;
    public JwtService(AppProperties properties){this.properties=properties;}
    @PostConstruct void initialize(){
        String secret=properties.getJwtSecret();
        if(secret==null || secret.getBytes(StandardCharsets.UTF_8).length<32 ||
           secret.equalsIgnoreCase("changeme") || secret.equalsIgnoreCase("your-secret-key") ||
           secret.chars().distinct().count()<16 || entropy(secret)<4.0 || isRepeatedPattern(secret))
            throw new IllegalStateException("JWT_SECRET must be a strong random secret of at least 32 bytes");
        key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String issue(SalesUser user){
        Instant now=Instant.now();
        return Jwts.builder().setSubject(user.getId().toString()).claim("username",user.getUsername()).claim("role",user.getRole())
            .setIssuedAt(Date.from(now)).setExpiration(Date.from(now.plusSeconds(8*60*60))).signWith(key).compact();
    }
    public Long userId(String token){
        Claims claims=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return Long.valueOf(claims.getSubject());
    }
    private double entropy(String value){
        Map<Integer,Long> counts=value.chars().boxed().collect(java.util.stream.Collectors.groupingBy(c->c,java.util.stream.Collectors.counting()));
        double result=0;
        for(long count:counts.values()){double p=(double)count/value.length();result-=p*(Math.log(p)/Math.log(2));}
        return result;
    }
    private boolean isRepeatedPattern(String value){
        for(int period=1;period<=value.length()/2;period++){
            if(value.length()%period!=0)continue;
            boolean repeated=true;
            for(int i=period;i<value.length();i++){
                if(value.charAt(i)!=value.charAt(i%period)){repeated=false;break;}
            }
            if(repeated)return true;
        }
        return false;
    }
}
