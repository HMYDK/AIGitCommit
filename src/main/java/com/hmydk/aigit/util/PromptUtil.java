package com.hmydk.aigit.util;

import com.hmydk.aigit.config.ApiKeySettings;

import java.util.List;

/**
 * PromptUtil
 *
 * @author hmydk
 */
public class PromptUtil {
    
    public static final String DEFAULT_PROMPT = getDefaultPrompt();
    
    public static String constructPrompt(String diff, String branch, List<String> historyMsg) {
        String content = ApiKeySettings.getInstance().getCustomPrompt().getPrompt();
        content = content.replace("{branch}", branch);
        if (content.contains("{history}") && historyMsg != null) {
            content = content.replace("{history}", String.join("\n", historyMsg));
        }
        
        if (content.contains("{diff}")) {
            content = content.replace("{diff}", diff);
        } else {
            content = content + "\n" + diff;
        }
        
        if (content.contains("{local}")) {
            content = content.replace("{local}", ApiKeySettings.getInstance().getCommitLanguage());
        }
        
        return content;
    }
    
    private static String getDefaultPrompt() {
        return """
                You are an AI assistant tasked with generating a Git commit message based on the provided code changes. Your goal is to create a clear, concise, and informative commit message that follows best practices.
                
                Input:
                - Branch name: {branch}
                - Code diff:
                ```
                {diff}
                ```
                - Recent commit history (for context):
                {history}
                
                Instructions:
                1. Analyze the provided code diff and branch name.
                2. Consider the context from the recent commit history.
                3. Generate a commit message following this format:
                   - First line: A short, imperative summary (50 characters or less)
                   - Blank line
                   - Detailed explanation (if necessary), wrapped at 72 characters
                4. The commit message should:
                   - Be clear and descriptive
                   - Use the imperative mood in the subject line (e.g., "Add feature" not "Added feature")
                   - Explain what and why, not how
                   - Reference relevant issue numbers if applicable
                   - Use {local} language
                5. Avoid:
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
                """;
    }
}
