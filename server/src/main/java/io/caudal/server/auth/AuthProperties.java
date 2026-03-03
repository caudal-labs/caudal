package io.caudal.server.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caudal.auth")
public record AuthProperties(
    boolean disabled,
    String apiKey
) {

    public AuthProperties {
        if (apiKey == null) {
            apiKey = "";
        }
    }
}
