package com.ecole.sales.auth;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class InvitationTokens {
    private InvitationTokens(){}
    public static String create(){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public static String hash(String token){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
