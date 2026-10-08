package com.ecole.sales.config;

import com.ecole.sales.user.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final SalesUserRepository users; private final AppProperties properties; private final PasswordEncoder encoder;
    public AdminBootstrap(SalesUserRepository users,AppProperties properties,PasswordEncoder encoder){this.users=users;this.properties=properties;this.encoder=encoder;}
    @Override public void run(String... args){
        if(users.count()!=0)return;
        String username=properties.getAdminUsername(),email=properties.getAdminEmail(),password=properties.getAdminInitialPassword();
        if(username==null||!username.matches("[a-z0-9][a-z0-9-]{0,47}")||email==null||!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||password==null||password.length()<12)
            throw new IllegalStateException("When initializing an empty database, ADMIN_USERNAME, ADMIN_EMAIL, and ADMIN_INITIAL_PASSWORD (at least 12 characters) must be configured");
        users.save(new SalesUser(username,username,email,encoder.encode(password),"PLATFORM_ADMIN",true));
    }
}
