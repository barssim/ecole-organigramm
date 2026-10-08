package com.ecole.sales.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String jwtSecret;
    private String adminUsername;
    private String adminEmail;
    private String adminInitialPassword;
    private String publicUrl;
    private String mailFrom;
    private int providerReadTimeoutSeconds = 120;
    private Providers providers = new Providers();
    public String getJwtSecret(){return jwtSecret;} public void setJwtSecret(String v){jwtSecret=v;}
    public String getAdminUsername(){return adminUsername;} public void setAdminUsername(String v){adminUsername=v;}
    public String getAdminEmail(){return adminEmail;} public void setAdminEmail(String v){adminEmail=v;}
    public String getAdminInitialPassword(){return adminInitialPassword;} public void setAdminInitialPassword(String v){adminInitialPassword=v;}
    public String getPublicUrl(){return publicUrl;} public void setPublicUrl(String v){publicUrl=v;}
    public String getMailFrom(){return mailFrom;} public void setMailFrom(String v){mailFrom=v;}
    public int getProviderReadTimeoutSeconds(){return providerReadTimeoutSeconds;}
    public void setProviderReadTimeoutSeconds(int v){providerReadTimeoutSeconds=v;}
    public Providers getProviders(){return providers;} public void setProviders(Providers v){providers=v;}
    public static class Providers {
        private Provider school = new Provider(); private Provider cafe = new Provider();
        public Provider getSchool(){return school;} public void setSchool(Provider v){school=v;}
        public Provider getCafe(){return cafe;} public void setCafe(Provider v){cafe=v;}
    }
    public static class Provider {
        private String url; private String key; private boolean enabled;
        public String getUrl(){return url;} public void setUrl(String v){url=v;}
        public String getKey(){return key;} public void setKey(String v){key=v;}
        public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
    }
}
