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

    // 文件忽略相关常量
    public static final String[] DEFAULT_EXCLUDE_PATTERNS = {
            "*.pb.go",           // Protocol Buffer生成文件
            "*.pb.cc",           // Protocol Buffer C++生成文件
            "*.pb.h",            // Protocol Buffer头文件
            "go.sum",            // Go依赖锁定文件
            "go.mod",            // Go模块文件（可选）
            "package-lock.json", // Node.js依赖锁定文件
            "yarn.lock",         // Yarn依赖锁定文件
            "pnpm-lock.yaml",    // PNPM依赖锁定文件
            "Cargo.lock",        // Rust依赖锁定文件
            "Pipfile.lock",      // Python依赖锁定文件
            "poetry.lock",       // Poetry依赖锁定文件
            "*.generated.*",     // 通用生成文件
            "*.gen.*",           // 生成文件简写
            "*_generated.*",     // 下划线生成文件
            "*_gen.*",           // 下划线生成文件简写
            "vendor/**",         // Go vendor目录
            "node_modules/**",   // Node.js依赖目录
            ".next/**",          // Next.js构建目录
            "dist/**",           // 构建输出目录
            "build/**",          // 构建目录
            "target/**",         // Maven/Rust构建目录
            "*.min.js",          // 压缩的JS文件
            "*.min.css",         // 压缩的CSS文件
            "*.bundle.*",        // 打包文件
            "*.chunk.*",         // 代码分块文件
            "coverage/**",       // 测试覆盖率目录
            ".nyc_output/**",    // NYC覆盖率输出
            "*.lcov",            // 覆盖率报告文件
            "*.log",             // 日志文件
            "*.tmp",             // 临时文件
            "*.temp",            // 临时文件
            ".DS_Store",         // macOS系统文件
            "Thumbs.db",         // Windows系统文件
            "*.swp",             // Vim交换文件
            "*.swo",             // Vim交换文件
            "*~"                 // 备份文件
    };

    // 将默认排除模式转换为文本格式
    public static final String DEFAULT_EXCLUDE_PATTERNS_TEXT = String.join("\n", DEFAULT_EXCLUDE_PATTERNS);

    public static final String EXCLUDE_PATTERNS_HELP_TEXT = 
            "<html>" +
            "<b>File Exclusion Rules:</b><br/>" +
            "• Supports wildcard patterns, e.g., *.pb.go matches all .pb.go files<br/>" +
            "• Supports directory patterns, e.g., vendor/** matches all files in vendor directory<br/>" +
            "• One rule per line, empty lines and lines starting with # are ignored<br/>" +
            "• Common generated files and dependency files are pre-configured, adjust as needed<br/>" +
            "<br/>" +
            "<b>Pre-configured rules include:</b><br/>" +
            "• Protocol Buffer generated files (*.pb.go, *.pb.cc, etc.)<br/>" +
            "• Dependency lock files (go.sum, package-lock.json, etc.)<br/>" +
            "• Build output directories (dist/, build/, target/, etc.)<br/>" +
            "• Compressed and bundled files (*.min.js, *.bundle.*, etc.)<br/>" +
            "• System and temporary files (.DS_Store, *.log, etc.)<br/>" +
            "</html>";

    public static String[] getAllPromptTypes() {
        return new String[]{PROJECT_PROMPT, CUSTOM_PROMPT};
    }

    public static final String Gemini = "Gemini";
    public static final String DeepSeek = "DeepSeek";
    public static final String Ollama = "Ollama";
    public static final String OpenAI_API = "OpenAI API";
    public static final String OpenAI_Compatible = "OpenAI Compatible";
    public static final String 阿里云百炼 = "阿里云百炼(Model Hub)";
    public static final String SiliconFlow = "SiliconFlow(Model Hub)";
    public static final String OpenRouter = "OpenRouter";
    public static final String CloudflareWorkersAI = "Cloudflare Workers AI";
    public static final String VolcEngine = "火山引擎(VolcEngine)";
    public static final String Kimi = "Kimi(Moonshot AI)";
    public static final String[] LLM_CLIENTS = {Gemini, DeepSeek, OpenAI_API, OpenAI_Compatible, OpenRouter, Ollama, 阿里云百炼, SiliconFlow, VolcEngine, CloudflareWorkersAI, Kimi};

    public static final Map<String, String[]> CLIENT_MODULES = new HashMap<>() {
        {
            put(DeepSeek, new String[]{"deepseek-chat"});
            put(Gemini, new String[]{"gemini-3.1-pro-preview", "gemini-3-flash-preview", "gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-1.5-pro", "gemini-1.5-flash", "gemini-flash-latest"});
            put(OpenAI_API, new String[]{""});
            put(OpenAI_Compatible, new String[]{""});
            put(SiliconFlow, new String[]{"deepseek-ai/DeepSeek-V3", "deepseek-ai/DeepSeek-V2.5", "Qwen/Qwen2.5-Coder-32B-Instruct"});
            put(Ollama, new String[]{"qwen2.5:14b", "llama3.2:3b"});
            put(CloudflareWorkersAI,
                    new String[]{"@cf/meta/llama-3.1-70b-instruct", "@cf/meta/llama-3.1-8b-instruct"});
            put(阿里云百炼, new String[]{"qwen-plus"});
            put(VolcEngine, new String[]{"deepseek-v3-241226"});
            put(OpenRouter, new String[]{"google/gemini-2.0-flash-exp:free", "meta-llama/llama-3.3-70b-instruct:free", "deepseek/deepseek-chat:free", "deepseek/deepseek-r1:free", "deepseek/deepseek-r1-zero:free"});
            put(Kimi, new String[]{"kimi-latest", "kimi-k2-0905-preview", "kimi-k1.5-preview", "moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"});
        }
    };

    public static Map<String, ApiKeySettings.ModuleConfig> moduleConfigs = new HashMap<>() {
        {
            put(Gemini, new ApiKeySettings.ModuleConfig("https://generativelanguage.googleapis.com/v1beta/models", ""));
            put(DeepSeek, new ApiKeySettings.ModuleConfig("https://api.deepseek.com/chat/completions", ""));
            put(Ollama, new ApiKeySettings.ModuleConfig("http://localhost:11434/api/generate", ""));
            put(OpenAI_API, new ApiKeySettings.ModuleConfig("https://{host}/v1/chat/completions", ""));
            put(OpenAI_Compatible, new ApiKeySettings.ModuleConfig("https://{host}/v1/chat/completions", "", ""));
            put(阿里云百炼, new ApiKeySettings.ModuleConfig("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", ""));
            put(SiliconFlow, new ApiKeySettings.ModuleConfig("https://api.siliconflow.cn/v1/chat/completions", ""));
            put(CloudflareWorkersAI, new ApiKeySettings.ModuleConfig(
                    "https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/v1/chat/completions", ""));
            put(VolcEngine, new ApiKeySettings.ModuleConfig("https://ark.cn-beijing.volces.com/api/v3/chat/completions", ""));
            put(OpenRouter, new ApiKeySettings.ModuleConfig("https://openrouter.ai/api/v1/chat/completions", ""));
            put(Kimi, new ApiKeySettings.ModuleConfig("https://api.moonshot.cn/v1/chat/completions", ""));
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
            put(Constants.OpenAI_Compatible, "https://platform.openai.com/docs/overview");
            put(Constants.VolcEngine, "https://www.volcengine.com/docs/82379");
            put(Constants.OpenRouter, "https://openrouter.ai/settings/keys");
            put(Constants.Kimi, "https://platform.moonshot.cn/console/api-keys");
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
            case OpenAI_Compatible -> "<html>" +
                    "<li>Use this option for providers that support the <a href=\"https://platform.openai.com/docs/api-reference/chat\">OpenAI-compatible API</a>.</li>" +
                    "<li>Specify the full endpoint URL, for example: https://{host}/v1/chat/completions, along with your provider’s API key.</li>" +
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
            case Kimi -> "<html>" +
                    "<li>Get your API key from <a href='https://platform.moonshot.cn/console/api-keys'>platform.moonshot.cn</a></li>" +
                    "<li>Current models include: kimi-latest, kimi-k2-0905-preview, kimi-k1.5-preview</li>" +
                    "<li>Kimi is optimized for Chinese language understanding and conversation</li>" +
                    "</html>";
            default -> "";
        };
    }
}
