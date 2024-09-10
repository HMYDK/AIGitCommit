package com.hmydk.aigit.util;

import com.hmydk.aigit.config.ApiKeySettings;
import com.hmydk.aigit.constant.Constants;

/**
 * PromptUtil
 *
 * @author hmydk
 */
public class PromptUtil {

    public static final String DEFAULT_PROMPT_1 = getDefaultPrompt();
    public static final String DEFAULT_PROMPT_2 = getPrompt3();

    public static String constructPrompt(String diff) {
        String promptContent = "";

        //get prompt content
        ApiKeySettings settings = ApiKeySettings.getInstance();
        if (Constants.PROJECT_PROMPT.equals(settings.getPromptType())) {
            promptContent = FileUtil.loadProjectPrompt();
        } else {
            promptContent = settings.getCustomPrompt().getPrompt();
        }

        //check prompt content
        if (!promptContent.contains("{diff}")) {
            throw new IllegalArgumentException("The project prompt file must contain the placeholder {diff}.");
        }
        if (!promptContent.contains("{language}")) {
            throw new IllegalArgumentException("The project prompt file must contain the placeholder {language}.");
        }

        //replace placeholder
        promptContent = promptContent.replace("{diff}", diff);
        promptContent = promptContent.replace("{language}", settings.getCommitLanguage());
        return promptContent;
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
                                
                Note: The final result should be given in {language}
                """;
    }

    private static String getPrompt3() {
        return """
                 Generate a concise yet detailed git commit message using the following format and information:
                                
                 ```
                 <type>(<scope>): <subject>
                                
                 <body>
                                
                 <footer>
                 ```
                               
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
                 ```
                 feat(user-auth): implement two-factor authentication
                                
                 • Add QR code generation for 2FA setup
                 • Integrate Google Authenticator API
                 • Update user settings for 2FA options
                 ```
                                
                 Notes:
                 - Keep the entire message under 300 characters
                 - Focus on what and why, not how
                 - Summarize diff to highlight key changes; don't include raw diff output
                                
                Note: The final result should be given in {language}
                """;
    }
}
