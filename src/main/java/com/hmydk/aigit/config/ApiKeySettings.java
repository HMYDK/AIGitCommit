package com.hmydk.aigit.config;

import com.hmydk.aigit.pojo.PromptInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "com.hmydk.aigit.config.ApiKeySettings", storages = { @Storage("AIGitCommitSettings.xml") })
public class ApiKeySettings implements PersistentStateComponent<ApiKeySettings> {
    private String aiModel = "Gemini";
    private String apiKey = "";
    private String commitLanguage = "English";
    private List<PromptInfo> customPrompts = new ArrayList<>();
    private PromptInfo customPrompt = new PromptInfo("", "");

    public static ApiKeySettings getInstance() {
        return ApplicationManager.getApplication().getService(ApiKeySettings.class);
    }

    @Nullable
    @Override
    public ApiKeySettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ApiKeySettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCommitLanguage() {
        return commitLanguage;
    }

    public void setCommitLanguage(String commitLanguage) {
        this.commitLanguage = commitLanguage;
    }

    public List<PromptInfo> getCustomPrompts() {
        if (customPrompts == null ||  customPrompts.isEmpty()){
            customPrompts = PromptInfo.defaultPrompts();
        }
        return customPrompts;
    }

    public void setCustomPrompts(List<PromptInfo> customPrompts) {
        this.customPrompts = customPrompts;
    }

    public PromptInfo getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(PromptInfo customPrompt) {
        this.customPrompt = customPrompt;
    }

}