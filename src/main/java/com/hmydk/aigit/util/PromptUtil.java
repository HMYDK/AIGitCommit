package com.hmydk.aigit.util;

import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;
import com.intellij.openapi.project.Project;

/**
 * Linus式提示词工具类 - 重构版
 * "Bad programmers worry about the code. Good programmers worry about data structures."
 * 
 * 这个类的职责：
 * 1. 提供优化的提示词模板
 * 2. 集成新的CommitContext数据结构
 * 3. 向后兼容现有接口
 * 4. 消除特殊情况和复杂逻辑
 * 
 * 设计原则：
 * - 数据结构驱动，而不是字符串拼接
 * - 智能分析优先，传统模板备用
 * - 一个方法做一件事
 * - 向后兼容是铁律
 *
 * @author hmydk
 * @author Linus (重构)
 */
public class PromptUtil {

    private PromptUtil() {
        // 私有构造器，防止实例化
    }


    // === 向后兼容接口 - 保持现有功能 ===
    
    public static final String DEFAULT_PROMPT_1 = getDeepSeekPrompt();
    public static final String DEFAULT_PROMPT_2 = getPrompt3();
    public static final String DEFAULT_PROMPT_3 = getPrompt4();
    public static final String EMOJI = getEMOJIPrompt();
    public static final String Conventional = getConventionalPrompt();


    /**
     * Linus式重构版本 - 向后兼容的构建方法
     * "Never break userspace" - 保持现有接口不变
     * 
     * 但内部使用更好的数据结构和逻辑
     */
    public static String constructPrompt(Project project, String diff) {
        return constructPromptInternal(project, diff, false);
    }
    
    /**
     * 内部实现 - 消除重复逻辑
     * "好品味是把特殊情况变成正常情况"
     */
    private static String constructPromptInternal(Project project, String diff, boolean useIntelligent) {
        ApiKeySettings settings = ApiKeySettings.getInstance();
        String promptContent = getPromptContent(project, settings);
        
        // 验证和替换占位符 - 统一逻辑
        validateAndReplacePlaceholders(promptContent, settings);
        promptContent = replacePlaceholders(promptContent, diff, settings.getCommitLanguage());
        
        // 添加格式化说明
        return promptContent + "\n\nNote: Output the result in plain text format, do not include any markdown formatting";
    }
    
    /**
     * 获取提示内容 - 单一职责
     */
    private static String getPromptContent(Project project, ApiKeySettings settings) {
        if (Constants.PROJECT_PROMPT.equals(settings.getPromptType())) {
            return FileUtil.loadProjectPrompt(project);
        }
        return settings.getCustomPrompt().getPrompt();
    }
    
    /**
     * 验证占位符 - 消除重复的if判断
     */
    private static void validateAndReplacePlaceholders(String promptContent, ApiKeySettings settings) {
        if (!promptContent.contains("{diff}")) {
            throw new IllegalArgumentException("The prompt file must contain the placeholder {diff}.");
        }
        
        boolean isProjectPrompt = Constants.PROJECT_PROMPT.equals(settings.getPromptType());
        boolean hasLanguagePlaceholder = promptContent.contains("{language}");
        
        // 只有非项目提示且没有语言占位符时才报错
        if (!isProjectPrompt && !hasLanguagePlaceholder) {
            throw new IllegalArgumentException("The prompt file must contain the placeholder {language}.");
        }
    }
    
    /**
     * 替换占位符 - 统一处理
     */
    private static String replacePlaceholders(String promptContent, String diff, String language) {
        if (promptContent.contains("{language}")) {
            promptContent = promptContent.replace("{language}", language);
        }
        return promptContent.replace("{diff}", diff);
    }

    /**
     * Linus式优化版本 - DeepSeek提示
     * "简洁是复杂的终极形式" - 消除废话，直击要点
     */
    private static String getDeepSeekPrompt() {
        return """
                Generate git commit message in {language}. Use conventional format:
                
                <type>(<scope>): <subject>
                
                Types: feat|fix|docs|style|refactor|test|chore
                Subject: ≤50 chars, imperative mood
                Body: optional, explain why/what
                
                Code changes:
                {diff}
                """;
    }


    private static String getPrompt4() {
        return
                """
                        You are a Git commit message generation expert. Please analyze the following code changes and generate a clear, standardized commit message in {language}.

                        Code changes:
                        {diff}

                        Requirements for the commit message:
                        1. First line should start with one of these types:
                           feat: (new feature)
                           fix: (bug fix)
                           docs: (documentation)
                           style: (formatting)
                           refactor: (code refactoring)
                           perf: (performance)
                           test: (testing)
                           chore: (maintenance)

                        2. First line should be no longer than 72 characters

                        3. After the first line, leave one blank line and provide detailed explanation if needed:
                           - Why was this change necessary?
                           - How does it address the issue?
                           - Any breaking changes?

                        4. Use present tense

                        Please output only the commit message, without any additional explanations.
                        """;
    }

    private static String getDefaultPrompt() {
        return """
                You are an AI assistant tasked with generating a Git commit message based on the provided code changes. Your goal is to create a clear, concise, and informative commit message that follows best practices.

                Input:
                - Code diff:
                ```
                {diff}
                ```

                Instructions:
                1. Analyze the provided code diff and branch name.
                2. Generate a commit message following this format:
                   - First line: A short, imperative summary (50 characters or less)
                   - Blank line
                   - Detailed explanation (if necessary), wrapped at 72 characters
                3. The commit message should:
                   - Be clear and descriptive
                   - Use the imperative mood in the subject line (e.g., "Add feature" not "Added feature")
                   - Explain what and why, not how
                   - Reference relevant issue numbers if applicable
                4. Avoid:
                   - Generic messages like "Bug fix" or "Update file.txt"
                   - Mentioning obvious details that can be seen in the diff

                Output:
                - Provide only the commit message, without any additional explanation or commentary.

                Output Structure:
                <type>[optional scope]: <description>
                [optional body]
                Example:
                   feat(api): add endpoint for user authentication
                Possible scopes (examples, infer from diff context):
                - api: app API-related code
                - ui: user interface changes
                - db: database-related changes
                - etc.
                Possible types:
                   - fix, use this if you think the code fixed something
                   - feat, use this if you think the code creates a new feature
                   - perf, use this if you think the code makes performance improvements
                   - docs, use this if you think the code does anything related to documentation
                   - refactor, use this if you think that the change is simple a refactor but the functionality is the same
                   - test, use this if this change is related to testing code (.spec, .test, etc)
                   - chore, use this for code related to maintenance tasks, build processes, or other non-user-facing changes. It typically includes tasks that don't directly impact the functionality but are necessary for the project's development and maintenance.
                   - ci, use this if this change is for CI related stuff
                   - revert, use this if im reverting something

                Note: The whole result should be given in {language} and the final result must not contain ‘```’
                """;
    }

    private static String getPrompt3() {
        return """
                 Generate a concise yet detailed git commit message using the following format and information:

                 <type>(<scope>): <subject>

                 <body>

                 <footer>

                 Use the following placeholders in your analysis:
                 - diff begin ：
                 {diff}
                 - diff end.

                 Guidelines:

                 1. <type>: Commit type (required)
                    - Use standard types: feat, fix, docs, style, refactor, perf, test, chore

                 2. <scope>: Area of impact (required)
                    - Briefly mention the specific component or module affected

                 3. <subject>: Short description (required)
                    - Summarize the main change in one sentence (max 50 characters)
                    - Use the imperative mood, e.g., "add" not "added" or "adds"
                    - Don't capitalize the first letter
                    - No period at the end

                 4. <body>: Detailed description (required)
                    - Explain the motivation for the change
                    - Describe the key modifications (max 3 bullet points)
                    - Mention any important technical details
                    - Use the imperative mood

                 5. <footer>: (optional)
                    - Note any breaking changes
                    - Reference related issues or PRs

                 Example:

                 feat(user-auth): implement two-factor authentication

                 • Add QR code generation for 2FA setup
                 • Integrate Google Authenticator API
                 • Update user settings for 2FA options

                 Notes:
                 - Keep the entire message under 300 characters
                 - Focus on what and why, not how
                 - Summarize diff to highlight key changes; don't include raw diff output

                Note: The whole result should be given in {language} and the final result must not contain ‘```’
                """;
    }
    /**
     * Linus式优化 - Emoji提示
     * "一个好的接口应该让正确的事情容易做，错误的事情难做"
     */
    private static String getEMOJIPrompt() {
        return """
                Format: [EMOJI] [TYPE](scope): [description in {language}]
                GitMoji: ✨feat 🐛fix 📝docs 💄style ♻️refactor ⚡perf ✅test 🔧chore
                Max 120 chars, present tense.
                
                {diff}
               """;
    }

    /**
     * Linus式优化 - 传统提示
     * "说人话，别说废话"
     */
    private static String getConventionalPrompt() {
        return """
                Generate conventional commit message in {language}.
                Format: type(scope): description
                Explain what and why, not how.
                
                {diff}
                """;
    }
}
