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

    public static final String[] languages = {"English", "Chinese", "Japanese", "Korean", "French", "Spanish",
            "German", "Russian", "Arabic", "Portuguese"};

    public static final String PROJECT_PROMPT_FILE_NAME = "commit-prompt.txt";
    public static final String PROJECT_PROMPT = "Project Prompt";
    public static final String CUSTOM_PROMPT = "Custom Prompt";

    public static String[] getAllPromptTypes() {
        return new String[]{PROJECT_PROMPT, CUSTOM_PROMPT};
    }

    public static final String Gemini = "Gemini";
    public static final String DeepSeek = "DeepSeek";
    public static final String Ollama = "Ollama";
    public static final String OpenAI_API = "OpenAI API";
    public static final String 阿里云百炼 = "阿里云百炼(Model Hub)";
    public static final String SiliconFlow = "SiliconFlow(Model Hub)";
    public static final String OpenRouter = "OpenRouter";
    public static final String CloudflareWorkersAI = "Cloudflare Workers AI";
    public static final String VolcEngine = "火山引擎(VolcEngine)";
    public static final String[] LLM_CLIENTS = {Gemini, DeepSeek, OpenAI_API, OpenRouter, Ollama, 阿里云百炼, SiliconFlow, VolcEngine, CloudflareWorkersAI};

    public static final Map<String, String[]> CLIENT_MODULES = new HashMap<>() {
        {
            put(DeepSeek, new String[]{"deepseek-chat"});
            put(Gemini, new String[]{"gemini-2.5-pro", "gemini-2.0-flash", "gemini-2.0-flash-lite-preview-02-05", "gemini-2.0-flash-exp", "gemini-1.5-flash-latest", "gemini-1.5-flash", "gemini-1.5-pro"});
            put(OpenAI_API, new String[]{""});
            put(SiliconFlow, new String[]{"deepseek-ai/DeepSeek-V3", "deepseek-ai/DeepSeek-V2.5", "Qwen/Qwen2.5-Coder-32B-Instruct"});
            put(Ollama, new String[]{"qwen2.5:14b", "llama3.2:3b"});
            put(CloudflareWorkersAI,
                    new String[]{"@cf/meta/llama-3.1-70b-instruct", "@cf/meta/llama-3.1-8b-instruct"});
            put(阿里云百炼, new String[]{"qwen-plus"});
            put(VolcEngine, new String[]{"deepseek-v3-241226"});
            put(OpenRouter, new String[]{"google/gemini-2.0-flash-exp:free", "meta-llama/llama-3.3-70b-instruct:free", "deepseek/deepseek-chat:free", "deepseek/deepseek-r1:free", "deepseek/deepseek-r1-zero:free"});
        }
    };

    public static Map<String, ApiKeySettings.ModuleConfig> moduleConfigs = new HashMap<>() {
        {
            put(Gemini, new ApiKeySettings.ModuleConfig("https://generativelanguage.googleapis.com/v1beta/models", ""));
            put(DeepSeek, new ApiKeySettings.ModuleConfig("https://api.deepseek.com/chat/completions", ""));
            put(Ollama, new ApiKeySettings.ModuleConfig("http://localhost:11434/api/generate", ""));
            put(OpenAI_API, new ApiKeySettings.ModuleConfig("https://{host}/v1/chat/completions", ""));
            put(阿里云百炼, new ApiKeySettings.ModuleConfig("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", ""));
            put(SiliconFlow, new ApiKeySettings.ModuleConfig("https://api.siliconflow.cn/v1/chat/completions", ""));
            put(CloudflareWorkersAI, new ApiKeySettings.ModuleConfig(
                    "https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/v1/chat/completions", ""));
            put(VolcEngine, new ApiKeySettings.ModuleConfig("https://ark.cn-beijing.volces.com/api/v3/chat/completions", ""));
            put(OpenRouter, new ApiKeySettings.ModuleConfig("https://openrouter.ai/api/v1/chat/completions", ""));
        }
    };

    public static final Map<String, String> CLIENT_HELP_URLS = new HashMap<>() {
        {
            put(Constants.Gemini, "https://aistudio.google.com/app/apikey");
            put(Constants.DeepSeek, "https://platform.deepseek.com/api_keys");
            put(Constants.CloudflareWorkersAI, "https://developers.cloudflare.com/workers-ai/get-started/rest-api");
            put(Constants.阿里云百炼, "https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key?spm=0.0.0.i7");
            put(Constants.SiliconFlow, "https://cloud.siliconflow.cn/i/lszKPlCW");
            put(Constants.OpenAI_API, "https://platform.openai.com/docs/overview");
            put(Constants.VolcEngine, "https://www.volcengine.com/docs/82379");
            put(Constants.OpenRouter, "https://openrouter.ai/settings/keys");
        }
    };

    public static String getHelpText(String client) {
        return switch (client) {
            case Gemini ->
                    "<html>Get your API key from <a href='https://aistudio.google.com/app/apikey'>Google AI Studio</a></html>";
            case DeepSeek -> "<html>" +
                    "<li>Get your API key from <a href='https://platform.deepseek.com/api_keys'>platform.deepseek.com</a></li>" +
                    "<li>Current model is deepseek-v3.</li>" +
                    "<li>DeepSeek servers is not stable currently.</li>" +
                    "</html>";
            case Ollama ->
                    "<html><li>Make sure Ollama is running locally on the specified URL</li><li>API Key is not required</li></html>";
            case OpenAI_API -> "<html>" +
                    "<li>Please confirm whether the current model supports the OpenAI API format.</li>"
                    +
                    "<li>Replace {host} with the host defined in the model.</li>" +
                    "<li>Refer to the API definition on the <a href='https://platform.openai.com/docs/overview'>OpenAI Platform</a>.</li>" +
                    "</html>";
            case CloudflareWorkersAI -> "<html>" +
                    "<li>Please refer to the <a href='https://developers.cloudflare.com/workers-ai/get-started/rest-api'>official documentation</a> for details</li>"
                    +
                    "<li>Replace {account_id} with your Cloudflare account ID</li>" +
                    "</html>";
            case 阿里云百炼 ->
                    "<html>Get your API key from <a href='https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key?spm=0.0.0.i7'>" + 阿里云百炼 + "</a></html>";
            case SiliconFlow ->
                    "<html>Get your API key from <a href='https://cloud.siliconflow.cn/i/lszKPlCW'>" + SiliconFlow + "</a></html>";
            case VolcEngine ->
                    "<html>Get your API key from <a href='https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey'>" + VolcEngine + "</a></html>";
            case OpenRouter -> "<html>" +
                    "<li>Get your API key from <a href='https://openrouter.ai/settings/keys'>" + OpenRouter + "</a></html></li>" +
                    "<li>Get free model from <a href='https://openrouter.ai/models?q=free'>here</a>.</li>" +
                    "</html>";
            default -> "";
        };
    }
}
