package com.hmydk.aigit.constant;

import com.hmydk.aigit.config.ApiKeySettings;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants
 *
 * @author hmydk
 */
public class Constants {

    public static final String NO_FILE_SELECTED = "No file selected";
    public static final String GENERATING_COMMIT_MESSAGE = "Generating commit message...";
    public static final String TASK_TITLE = "Generating commit message";

    public static final String[] languages = { "English", "Chinese", "Japanese", "Korean", "French", "Spanish",
            "German", "Russian", "Arabic", "Portuguese" };

    public static final String PROJECT_PROMPT_FILE_NAME = "commit-prompt.txt";
    public static final String PROJECT_PROMPT = "Project Prompt";
    public static final String CUSTOM_PROMPT = "Custom Prompt";

    public static String[] getAllPromptTypes() {
        return new String[] { PROJECT_PROMPT, CUSTOM_PROMPT };
    }

    public static final String Gemini = "Gemini";
    public static final String Ollama = "Ollama";
    public static final String OpenAI = "OpenAI";
    public static final String 阿里云百炼 = "阿里云百炼";
    public static final String CloudflareWorkersAI = "Cloudflare Workers AI";

    public static final String[] LLM_CLIENTS = {Gemini, Ollama, CloudflareWorkersAI, 阿里云百炼};

    public static final Map<String, String[]> CLIENT_MODULES = new HashMap<>() {
        {
            put(Gemini, new String[]{"gemini-1.5-flash-latest", "gemini-1.5-flash", "gemini-1.5-pro"});
            put(OpenAI, new String[]{"gpt-4o-mini"});
            put(Ollama, new String[]{"qwen2.5:14b", "llama3.2:3b"});
            put(CloudflareWorkersAI,
                    new String[]{"@cf/meta/llama-3.1-70b-instruct", "@cf/meta/llama-3.1-8b-instruct"});
            put(阿里云百炼, new String[]{"qwen-plus"});
        }
    };

    public static Map<String, ApiKeySettings.ModuleConfig> moduleConfigs = new HashMap<>() {
        {
            put(Gemini, new ApiKeySettings.ModuleConfig("https://generativelanguage.googleapis.com/v1beta/models", ""));
            put(Ollama, new ApiKeySettings.ModuleConfig("http://localhost:11434/api/generate", ""));
            put(OpenAI, new ApiKeySettings.ModuleConfig("https://api.openai.com/v1/chat/completions", ""));
            put(阿里云百炼, new ApiKeySettings.ModuleConfig("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", ""));
            put(CloudflareWorkersAI, new ApiKeySettings.ModuleConfig(
                    "https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/v1/chat/completions", ""));
        }
    };

    public static String getHelpText(String client) {
        return switch (client) {
            case Gemini ->
                "<html>Get your API key from <a href='https://aistudio.google.com/app/apikey'>Google AI Studio</a></html>";
            case Ollama ->
                "<html><li>Make sure Ollama is running locally on the specified URL</li><li>API Key is not required</li></html>";
            case OpenAI -> "https://platform.openai.com/api-keys";
            case CloudflareWorkersAI -> "<html>" +
                    "<li>Please refer to the <a href='https://developers.cloudflare.com/workers-ai/get-started/rest-api'>official documentation</a> for details</li>"
                    +
                    "<li>Replace {account_id} with your Cloudflare account ID</li>" +
                    "</html>";
            case 阿里云百炼 ->
                    "<html>Get your API key from <a href='https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key?spm=0.0.0.i7'>阿里云百炼</a></html>";
            default -> "";
        };
    }
}
