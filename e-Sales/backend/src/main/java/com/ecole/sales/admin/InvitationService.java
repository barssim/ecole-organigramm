package com.ecole.sales.admin;

import com.ecole.sales.auth.InvitationTokens;
import com.ecole.sales.common.ApiException;
import com.ecole.sales.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class InvitationService {
    private final SalesUserRepository users;private final InvitationRepository invitations;private final PasswordEncoder encoder;
    public InvitationService(SalesUserRepository users,InvitationRepository invitations,PasswordEncoder encoder){
        this.users=users;this.invitations=invitations;this.encoder=encoder;
    }
    public record Prepared(SalesUser user,String token){}
    @Transactional public Prepared create(String username,String name,String email){
        if(users.existsByUsername(username))throw new ApiException(HttpStatus.CONFLICT,"Username is already in use");
        SalesUser user=users.save(new SalesUser(username,name,email,encoder.encode(UUID.randomUUID().toString()),"SALES_USER",false));
        return rotate(user);
    }
    @Transactional public Prepared rotate(long id){
        SalesUser user=users.lockById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Sales user not found"));
        if(!"SALES_USER".equals(user.getRole()))throw new ApiException(HttpStatus.NOT_FOUND,"Sales user not found");
        return rotate(user);
    }
    private Prepared rotate(SalesUser user){
        invitations.invalidateForUser(user.getId(),Instant.now());
        String token=InvitationTokens.create();
        invitations.save(new Invitation(user,InvitationTokens.hash(token),Instant.now().plus(7,ChronoUnit.DAYS)));
        return new Prepared(user,token);
    }
}
