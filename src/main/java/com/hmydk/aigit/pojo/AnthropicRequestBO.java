package com.hmydk.aigit.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for the Anthropic Messages API.
 *
 * @see <a href="https://platform.claude.com/docs/en/api/messages">Anthropic Messages API</a>
 */
public class AnthropicRequestBO {

    @JsonProperty("model")
    private String model;

    @JsonProperty("max_tokens")
    private int maxTokens;

    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("stream")
    private boolean stream;

    public AnthropicRequestBO() {
    }

    public AnthropicRequestBO(String model, int maxTokens, String prompt, boolean stream) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.messages = List.of(new Message("user", prompt));
        this.stream = stream;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public static class Message {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
