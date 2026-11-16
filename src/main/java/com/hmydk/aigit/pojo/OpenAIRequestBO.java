package com.hmydk.aigit.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenAIRequestBO {

    private String model;

    private List<OpenAIRequestMessage> messages;

    private boolean stream;

    /**
     * Optional OpenAI reasoning effort ({@code low}/{@code medium}/{@code high}). Omitted from the
     * JSON payload when unset so the default request shape is unchanged.
     */
    @SerializedName("reasoning_effort")
    private String reasoningEffort;

    public static class OpenAIRequestMessage {

        private String role;
        private String content;

        public OpenAIRequestMessage(String role, String content) {
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<OpenAIRequestMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<OpenAIRequestMessage> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }
}
