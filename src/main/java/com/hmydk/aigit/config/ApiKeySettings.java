package com.hmydk.aigit.config;

import com.hmydk.aigit.constant.Constants;
import com.hmydk.aigit.pojo.PromptInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@State(name = "com.hmydk.aigit.config.ApiKeySettings", storages = { @Storage("AIGitCommitSettings.xml") })
public class ApiKeySettings implements PersistentStateComponent<ApiKeySettings> {
    private String selectedClient = "Gemini";
    private String selectedModule = "gemini-3-flash-preview";
    private String commitLanguage = "English";

    private String promptType = Constants.CUSTOM_PROMPT;

    // prompt from table
    private List<PromptInfo> customPrompts = new ArrayList<>();

    // current prompt by user choose
    private PromptInfo customPrompt = new PromptInfo("", "");

    private Map<String, ModuleConfig> moduleConfigs = new HashMap<>();

    // 文件忽略规则配置
    private boolean enableFileExclusion = false; // 是否启用文件忽略功能
    private List<String> excludePatterns = new ArrayList<>(Arrays.asList(Constants.DEFAULT_EXCLUDE_PATTERNS)); // 忽略规则列表

    private boolean useSystemProxy = false; // 是否使用系统代理

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

    public String getSelectedClient() {
        return selectedClient;
    }

    public void setSelectedClient(String selectedClient) {
        this.selectedClient = selectedClient;
    }

    public String getCommitLanguage() {
        return commitLanguage;
    }

    public void setCommitLanguage(String commitLanguage) {
        this.commitLanguage = commitLanguage;
    }

    public List<PromptInfo> getCustomPrompts() {
        if (customPrompts == null || customPrompts.isEmpty()) {
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

    public String getPromptType() {
        return promptType;
    }

    public void setPromptType(String promptType) {
        this.promptType = promptType;
    }

    public String getSelectedModule() {
        return selectedModule;
    }

    public void setSelectedModule(String selectedModule) {
        this.selectedModule = selectedModule;
    }

    public Map<String, ModuleConfig> getModuleConfigs() {
        return moduleConfigs;
    }

    public void setModuleConfigs(Map<String, ModuleConfig> moduleConfigs) {
        this.moduleConfigs = moduleConfigs;
    }

    // 文件忽略功能的getter和setter方法
    public boolean isEnableFileExclusion() {
        return enableFileExclusion;
    }

    public void setEnableFileExclusion(boolean enableFileExclusion) {
        this.enableFileExclusion = enableFileExclusion;
    }

    public List<String> getExcludePatterns() {
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            excludePatterns = new ArrayList<>(Arrays.asList(Constants.DEFAULT_EXCLUDE_PATTERNS));
        }
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    /**
     * 重置为默认忽略规则
     */
    public void resetToDefaultExcludePatterns() {
        this.excludePatterns = new ArrayList<>(Arrays.asList(Constants.DEFAULT_EXCLUDE_PATTERNS));
    }

    public boolean isUseSystemProxy() {
        return useSystemProxy;
    }

    public void setUseSystemProxy(boolean useSystemProxy) {
        this.useSystemProxy = useSystemProxy;
    }

    public static class ModuleConfig {
        private String url;
        private String apiKey;
        private String modelId; // model id for OpenAI Compatible

        public ModuleConfig() {
        }

        public ModuleConfig(String url, String apiKey) {
            this.url = url;
            this.apiKey = apiKey;
        }

        public ModuleConfig(String url, String apiKey, String modelId) {
            this.url = url;
            this.apiKey = apiKey;
            this.modelId = modelId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }
    }
}