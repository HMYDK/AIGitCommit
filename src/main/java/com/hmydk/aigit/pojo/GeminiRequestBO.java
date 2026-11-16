package com.hmydk.aigit.pojo;

import java.util.List;

/**
 * GeminiRequestBO
 *
 * @author hmydk
 */
public class GeminiRequestBO {
    private List<Content> contents;

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
