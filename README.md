<div align="center">
    <a href="https://plugins.jetbrains.com/plugin/24851-ai-git-commit">
        <img src="./src/main/resources/META-INF/pluginIcon.svg" width="200" height="200" alt="AI Git Commit logo"/>
    </a>
</div>

<h1 align="center">AI Git Commit</h1>

<p align="center">
    <a href="https://plugins.jetbrains.com/plugin/24851-ai-git-commit"><img src="https://img.shields.io/jetbrains/plugin/d/24851-ai-git-commit.svg?style=flat-square" alt="JetBrains Marketplace downloads"/></a>
    <a href="https://plugins.jetbrains.com/plugin/24851-ai-git-commit"><img src="https://img.shields.io/jetbrains/plugin/v/24851-ai-git-commit.svg?style=flat-square" alt="JetBrains Marketplace version"/></a>
</p>

## Overview

AI Git Commit is a JetBrains IDE plugin that analyzes the changes selected in the Commit tool window and generates a clear commit message with the AI provider and model you configure.

## Features

- Generate commit messages from selected files, unversioned files, and selected commit hunks.
- Compact large change sets automatically to stay within model context limits.
- Choose the output language and use editable model IDs.
- Create, edit, and reuse multiple custom prompt templates.
- Load a project-specific prompt from `commit-prompt.txt` in the project root.
- Exclude generated files, lock files, build output, or custom wildcard patterns.
- Use the IDE system proxy when required.
- Review and copy the most recently used prompt.
- Stream generated text when the selected provider supports streaming.

Project prompt files must contain the `{diff}` placeholder and may use `{language}` for the selected output language.

## Supported AI providers and gateways

- [OrcaRouter](https://www.orcarouter.ai/ref/ref_e708d0dd88ba83cbccde) (free models available)
- [Google Gemini](https://aistudio.google.com/app/apikey)
- [DeepSeek](https://platform.deepseek.com/api_keys)
- [OpenAI API](https://developers.openai.com/api/docs)
- OpenAI-compatible providers with a custom endpoint, API key, and model ID
- [OpenRouter](https://openrouter.ai/)
- [Ollama](https://ollama.com/)
- [Alibaba Cloud Model Studio (百炼)](https://help.aliyun.com/zh/model-studio/get-api-key)
- [SiliconFlow](https://cloud.siliconflow.cn/i/lszKPlCW)
- [VolcEngine](https://volcengine.com/L/QpwJ2INEat4/)
- [Cloudflare Workers AI](https://developers.cloudflare.com/workers-ai/)
- [Kimi (Moonshot AI)](https://platform.kimi.com/console/api-keys)

The model selector is editable. Model availability and pricing are controlled by each provider and may change over time.

## Installation

AI Git Commit requires a JetBrains IDE based on IntelliJ Platform 2024.1 or later.

Install it from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/24851-ai-git-commit), or open **Settings/Preferences > Plugins > Marketplace**, search for **AI Git Commit**, and select **Install**.

<a href="https://plugins.jetbrains.com/plugin/24851-ai-git-commit">
    <img src="https://user-images.githubusercontent.com/12044174/123105697-94066100-d46a-11eb-9832-338cdf4e0612.png" width="300" alt="Install AI Git Commit from the JetBrains Marketplace"/>
</a>

## Quick start

1. Open **Settings/Preferences > Tools > AI Git Commit**.
2. Select an AI provider, model, and commit-message language.
3. Open the provider settings, then enter the endpoint and API key if required.
4. Optionally configure custom prompts, file filtering, and the system proxy.
5. In the Commit tool window, select the changes to include and choose **Generate AI Commit Message**.

## Privacy

Generating a commit message sends the selected change context and prompt to the AI service you configured. Review that provider's privacy and data-retention policies before sending sensitive source code. Use Ollama if you prefer a locally hosted model.

## Development

The project targets Java 17 and IntelliJ Platform 2024.1.

```bash
./gradlew check
./gradlew buildPlugin
```

See [IDE compatibility](docs/compatibility.md) for supported IDE profiles and release verification steps.

## Support

- [Report a bug or request a feature](https://github.com/HMYDK/AIGitCommit/issues)
- [Browse and share prompt templates](https://github.com/HMYDK/AIGitCommit/discussions/23)

## License

Licensed under the [GNU General Public License v3.0](LICENSE).
