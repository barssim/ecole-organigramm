package com.ecole.sales.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    public JwtFilter(JwtService jwt){this.jwt=jwt;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        String header=req.getHeader("Authorization");
        if(header!=null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication()==null){
            try {
                Long id=jwt.userId(header.substring(7));
                var auth=new UsernamePasswordAuthenticationToken(id,null,List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch(JwtException|IllegalArgumentException ex) {
                res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Invalid or expired token\"}");return;
            }
        }
        chain.doFilter(req,res);
    }
}
