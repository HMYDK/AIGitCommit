package com.hmydk.aigit.pojo;

import java.util.List;

public class OpenAIRequestBO {

    private String model;

    private List<OpenAIRequestMessage> messages;

    private boolean stream;

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
}
