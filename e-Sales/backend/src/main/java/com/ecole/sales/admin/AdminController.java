package com.ecole.sales.admin;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.config.AppProperties;
import com.ecole.sales.customer.CustomerOwnershipRepository;
import com.ecole.sales.mail.InvitationMailer;
import com.ecole.sales.security.*;
import com.ecole.sales.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final CurrentUser current; private final SalesUserRepository users;
    private final CustomerOwnershipRepository ownership; private final AppProperties props; private final InvitationMailer mailer;private final InvitationService invitationService;
    private final SalesUserCleanupService cleanup;
    public AdminController(CurrentUser current,SalesUserRepository users,CustomerOwnershipRepository ownership,AppProperties props,InvitationMailer mailer,InvitationService invitationService,SalesUserCleanupService cleanup){
        this.current=current;this.users=users;this.ownership=ownership;this.props=props;this.mailer=mailer;this.invitationService=invitationService;this.cleanup=cleanup;
    }
    public record SalesView(Long id,String username,String name,String email,String role,boolean active){}
    public record CreateRequest(@NotBlank @Pattern(regexp="[a-zA-Z0-9][a-zA-Z0-9-]{0,47}") String username,
                                @NotBlank @Size(max=160) String name,@NotBlank @Email @Size(max=254) String email){}
    public record InviteResponse(Long id,String username,String name,String email,String role,boolean active,String invitationLink,boolean invitationEmailSent){}
    @GetMapping("/sales-users") public List<SalesView> list(){
        current.requireAdmin(current.get());return users.findAllByRoleOrderByUsername("SALES_USER").stream().map(this::view).toList();
    }
    @PostMapping("/sales-users") public ResponseEntity<InviteResponse> create(@Valid @RequestBody CreateRequest req){
        current.requireAdmin(current.get());
        InvitationService.Prepared p=invitationService.create(req.username().toLowerCase(Locale.ROOT),req.name(),req.email());
        return ResponseEntity.status(201).body(inviteResponse(p));
    }
    @PostMapping("/sales-users/{id}/invitation") public InviteResponse resend(@PathVariable long id){
        current.requireAdmin(current.get());
        return inviteResponse(invitationService.rotate(id));
    }
    @DeleteMapping("/sales-users/{id}") public ResponseEntity<Void> delete(@PathVariable long id){
        current.requireAdmin(current.get());SalesUser u=users.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Sales user not found"));
        if(!"SALES_USER".equals(u.getRole()))throw new ApiException(HttpStatus.NOT_FOUND,"Sales user not found");
        if(ownership.existsByOwnerId(id))throw new ApiException(HttpStatus.CONFLICT,"Sales user owns customers; reassign them before deletion");
        cleanup.cleanBeforeRemoval(u);
        users.delete(u);return ResponseEntity.noContent().build();
    }
    private InviteResponse inviteResponse(InvitationService.Prepared p){
        String link=props.getPublicUrl().replaceAll("/+$","")+"/accept-invitation?token="+java.net.URLEncoder.encode(p.token(),java.nio.charset.StandardCharsets.UTF_8);
        InvitationMailer.Result delivery=mailer.send(p.user(),link);SalesView v=view(p.user());
        return new InviteResponse(v.id(),v.username(),v.name(),v.email(),v.role(),v.active(),link,delivery.sent());
    }
    private SalesView view(SalesUser u){return new SalesView(u.getId(),u.getUsername(),u.getName(),u.getEmail(),u.getRole(),u.isActive());}
}
