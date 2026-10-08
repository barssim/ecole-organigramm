package com.ecole.sales.security;

import com.ecole.sales.config.AppProperties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtSecretTest {
    @Test void rejectsWeakAndAcceptsRandomSecrets(){
        AppProperties properties=new AppProperties();
        properties.setJwtSecret("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        JwtService weak=new JwtService(properties);
        assertThrows(IllegalStateException.class,weak::initialize);
        properties.setJwtSecret("0123456789abcdef".repeat(4));
        assertThrows(IllegalStateException.class,()->new JwtService(properties).initialize());
        properties.setJwtSecret("F7wP4aZ8qM2xK6nR9cV3hJ5sD1bL0uYe");
        assertDoesNotThrow(()->new JwtService(properties).initialize());
    }
}
