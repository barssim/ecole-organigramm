package com.ecole.sales.security;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final SalesUserRepository users;
    public CurrentUser(SalesUserRepository users){this.users=users;}
    public SalesUser get(){
        Object id=SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SalesUser user=users.findById(Long.valueOf(id.toString())).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Account not found"));
        if(!user.isActive())throw new ApiException(HttpStatus.UNAUTHORIZED,"Account is inactive");
        return user;
    }
    public boolean isAdmin(SalesUser user){return "PLATFORM_ADMIN".equals(user.getRole());}
    public void requireAdmin(SalesUser user){if(!isAdmin(user))throw new ApiException(HttpStatus.FORBIDDEN,"Platform administrator access required");}
}
