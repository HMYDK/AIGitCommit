package com.hmydk.aigit.config;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "com.hmydk.aigit.config.ApiKeySettings",
        storages = {@Storage("AIGitCommitSettings.xml")}
)
public class ApiKeySettings implements PersistentStateComponent<ApiKeySettings> {
    private String aiModel = "Gemini";
    private String apiKey = "";
    private String commitLanguage = "English";

    public static ApiKeySettings getInstance() {
        return ServiceManager.getService(ApiKeySettings.class);
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
}