package com.example.demo_app.login;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class ApplicationProperties {
    private String secretKey;
    private int expirationTimeInHours = 1;

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public int getExpirationTimeInHours() { return expirationTimeInHours; }
    public void setExpirationTimeInHours(int expirationTimeInHours) { this.expirationTimeInHours = expirationTimeInHours; }
}
