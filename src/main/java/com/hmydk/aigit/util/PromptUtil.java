package com.hmydk.aigit.util;

import java.util.List;

/**
 * PromptUtil
 *
 * @author hmydk
 */
public class PromptUtil {

    public static String constructPrompt(String diff, String branch, List<String> historyMsg) {
        String content = generatePrompt3();
        content = content.replace("{branch}", branch);
        if (content.contains("{history}") && historyMsg != null) {
            content = content.replace("{history}", String.join("\n", historyMsg));
        }

        if (content.contains("{diff}")) {
            content = content.replace("{diff}", diff);
        } else {
            content = content + "\n" + diff;
        }

        return content;
    }

    private static String generatePrompt4() {
        return """
                 Generate a commit message using the Angular Conventional Commit Convention.
                 Constraints:
                 - Summarize changes with specificity
                 - Optionally include benefits in the body
                 - Use emojis for expression
                 - Keep lines within 72 characters
                 - Use English language
                 - End the commit title with issue number from {branch} if available
                 - Infer the scope from the context of the diff
                 Structure:
                 <type>[optional scope]: <description>
                 [optional body]
                 [optional footer]
                 Example:
                 ✨ feat(api): add endpoint for user authentication
                 Possible scopes (examples, infer from diff context):
                 - api: app API-related code
                 - ui: user interface changes
                 - db: database-related changes
                 - etc.
                 Possible types:
                 - 🐛 fix
                 - ✨ feat
                 - 📝 docs
                 - 🧹 refactor
                 - 🚀 perf
                 - 🔒 security
                 - 🚧 chore
                 - 🧪 test
                 Diff:
                 {diff}
                """;
    }

    private static String generatePrompt3() {
        return """
                Write a meaningful commit message in the Angular Conventional Commit Convention by summing up, thus being specific, to what changed. If you can figure out the benefits of the code, you may add add this to the commit body. I'll send you an output of 'git diff --staged' command, and you convert it into a commit message. Lines must not be longer than 74 characters. Use {locale} language to answer. End commit title with issue number if only and only if you can get it from the branch name: {branch} in parenthesis, else don't do this. Do not use any emojis!
                             
                Your commit message should follow the following style, example:\s
                              
                refactor(api): a description here
                              
                1:An optional body text here
                              
                2:An optional footer text here
                              
                The commit description (which comes right after the "type(scope):" must not be sentence-case, start-case, pascal-case, upper-case [subject-case] and not end with a period (.) and must not be over 74 characters in length.
                              
                "refactor" is the type, I'll list all possible types.                              
                          
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
                              
                This is the diff:
                {diff}
                  """;
    }

    private static String generatePrompt2() {
        return """
                 Given the diff below, create a concise commit message following the Conventional Commits format (e.g., fix: correct minor typos in code). The primary change description should be prioritized. Avoid verbosity:
                 {diff}
                """;
    }

    private static String generatePrompt() {
        return """
                I'll send you an output of 'git diff --staged' command, and you convert it into a commit message.
                remember these：
                - now branch is {branch}
                - Determine the language to be used based on the given historical commit msg
                - Based on the given historical commit msg, summarize the format, specifications, tone and intonation of the msg
                - Imitate the language and style of historical git msg to write new git msg
                - Lines must not be longer than 74 characters.
                - Generate the appropriate git commit msg directly for me without any other unnecessary explanations
                            
                            
                historical git msg is below
                ------------------------
                {history}
                            
                            
                git diff detail is below
                ------------------------
                {diff}
                            
                            
                so, git msg content is
                -----------------------
                """;
    }
}
