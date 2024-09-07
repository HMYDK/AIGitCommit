package com.hmydk.aigit.pojo;

/**
 * PromptInfo
 *
 * @author hmydk
 */
public class PromptInfo {
    private String description = "";
    private String prompt = "";

    public PromptInfo(String description, String prompt) {
        this.description = description;
        this.prompt = prompt;
    }

    public String getDescription() {
        return description;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
