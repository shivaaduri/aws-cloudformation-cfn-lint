package com.swebench.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PRData {
    private Base base;
    private String body;

    // Getters and setters
    public static class Base {
        @JsonProperty("sha")
        private String sha;

        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
    }

    public Base getBase() { return base; }
    public void setBase(Base base) { this.base = base; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}