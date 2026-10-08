package com.ecole.sales.user;

import jakarta.persistence.*;

@Entity @Table(name="sales_user")
public class SalesUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=48) private String username;
    @Column(nullable=false, length=160) private String name;
    @Column(nullable=false, length=254) private String email;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(nullable=false, length=32) private String role;
    @Column(nullable=false) private boolean active=true;
    protected SalesUser() {}
    public SalesUser(String username,String name,String email,String passwordHash,String role,boolean active){
        this.username=username;this.name=name;this.email=email;this.passwordHash=passwordHash;this.role=role;this.active=active;
    }
    public Long getId(){return id;} public String getUsername(){return username;} public String getName(){return name;}
    public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getRole(){return role;}
    public boolean isActive(){return active;} public void setPasswordHash(String v){passwordHash=v;} public void setActive(boolean v){active=v;}
}
