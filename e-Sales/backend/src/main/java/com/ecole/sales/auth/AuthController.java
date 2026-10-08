package com.ecole.sales.auth;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.security.*;
import com.ecole.sales.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;

@RestController @RequestMapping("/api")
public class AuthController {
    private final SalesUserRepository users; private final InvitationRepository invitations;
    private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthController(SalesUserRepository users,InvitationRepository invitations,PasswordEncoder encoder,JwtService jwt){this.users=users;this.invitations=invitations;this.encoder=encoder;this.jwt=jwt;}
    public record LoginRequest(@NotBlank String username,@NotBlank String password){}
    public record AcceptRequest(@NotBlank String token,@NotBlank @Size(min=12,max=200) String password){}
    public record UserView(Long id,String username,String name,String email,String role){}
    public record LoginResponse(String token,UserView user){}
    @PostMapping("/auth/login") public LoginResponse login(@Valid @RequestBody LoginRequest request){
        SalesUser user=users.findByUsername(request.username().toLowerCase()).orElse(null);
        if(user==null||!user.isActive()||!encoder.matches(request.password(),user.getPasswordHash()))throw new ApiException(HttpStatus.UNAUTHORIZED,"Invalid username or password");
        return response(user);
    }
    @PostMapping("/auth/invitations/accept") @Transactional public LoginResponse accept(@Valid @RequestBody AcceptRequest request){
        Invitation invite=invitations.lockByTokenHash(InvitationTokens.hash(request.token())).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Invitation is invalid or expired"));
        if(invite.getUsedAt()!=null||!invite.getExpiresAt().isAfter(Instant.now()))throw new ApiException(HttpStatus.BAD_REQUEST,"Invitation is invalid or expired");
        SalesUser user=invite.getUser(); user.setPasswordHash(encoder.encode(request.password()));user.setActive(true);
        invite.setUsedAt(Instant.now());return response(user);
    }
    @GetMapping("/me") public UserView me(@CurrentUserArg SalesUser user){return view(user);}
    private LoginResponse response(SalesUser user){return new LoginResponse(jwt.issue(user),view(user));}
    private UserView view(SalesUser u){return new UserView(u.getId(),u.getUsername(),u.getName(),u.getEmail(),u.getRole());}
}
