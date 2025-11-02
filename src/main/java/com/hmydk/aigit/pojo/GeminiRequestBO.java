package com.hmydk.aigit.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GeminiRequestBO
 *
 * @author hmydk
 */
public class GeminiRequestBO {
    @JsonProperty("contents")
    private List<Content> contents;

    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;


    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }


    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        @JsonProperty("text")
        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }


    public static class GenerationConfig {
        @JsonProperty("thinkingConfig")
        private ThinkingConfig thinkingConfig;

        public GenerationConfig(ThinkingConfig thinkingConfig) {
            this.thinkingConfig = thinkingConfig;
        }

        public ThinkingConfig getThinkingConfig() {
            return thinkingConfig;
        }

        public void setThinkingConfig(ThinkingConfig thinkingConfig) {
            this.thinkingConfig = thinkingConfig;
        }

    }

    public static class ThinkingConfig {
        public Integer thinkingBudget;

        public ThinkingConfig(Integer thinkingBudget) {
            this.thinkingBudget = thinkingBudget;
        }

        public Integer getThinkingBudget() {
            return thinkingBudget;
        }

        public void setThinkingBudget(Integer thinkingBudget) {
            this.thinkingBudget = thinkingBudget;
        }
    }
}
