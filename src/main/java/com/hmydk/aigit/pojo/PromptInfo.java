package com.hmydk.aigit.pojo;

import com.hmydk.aigit.util.PromptUtil;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * PromptInfo
 *
 * @author hmydk
 */
public class PromptInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String description = "";
    private String prompt = "";

    public PromptInfo() {
    }

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

    public static List<PromptInfo> defaultPrompts() {
        List<PromptInfo> prompts = new ArrayList<>();
        prompts.add(new PromptInfo("Default", PromptUtil.DEFAULT_PROMPT_1));
        prompts.add(new PromptInfo("Detailed", PromptUtil.DEFAULT_PROMPT_2));
        prompts.add(new PromptInfo("Perfect", PromptUtil.DEFAULT_PROMPT_3));
        return prompts;
    }
}
