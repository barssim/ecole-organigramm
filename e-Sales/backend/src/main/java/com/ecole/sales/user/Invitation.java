package com.ecole.sales.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="invitation")
public class Invitation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private SalesUser user;
    @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="used_at") private Instant usedAt;
    protected Invitation(){}
    public Invitation(SalesUser user,String tokenHash,Instant expiresAt){this.user=user;this.tokenHash=tokenHash;this.expiresAt=expiresAt;}
    public SalesUser getUser(){return user;} public String getTokenHash(){return tokenHash;} public Instant getExpiresAt(){return expiresAt;}
    public Instant getUsedAt(){return usedAt;} public void setUsedAt(Instant value){usedAt=value;}
}
