package com.github.macgregor.alexandria.remotes;

import java.util.Optional;

public class RestRemoteConfig{
    private String baseUrl;
    private Optional<String> oauthToken = Optional.empty();
    private Optional<String> username = Optional.empty();
    private Optional<String> password = Optional.empty();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Optional<String> getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(Optional<String> oauthToken) {
        this.oauthToken = oauthToken;
    }

    public Optional<String> getUsername() {
        return username;
    }

    public void setUsername(Optional<String> username) {
        this.username = username;
    }

    public Optional<String> getPassword() {
        return password;
    }

    public void setPassword(Optional<String> password) {
        this.password = password;
    }
}
