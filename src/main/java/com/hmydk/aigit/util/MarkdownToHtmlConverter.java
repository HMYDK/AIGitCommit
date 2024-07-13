package com.hmydk.aigit.util;

/**
 * MarkdownToHtmlConverter
 *
 * @author hmydk
 */
public class MarkdownToHtmlConverter {
    public static String convertToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "ai response is empty";
        }

        StringBuilder html = new StringBuilder();

        // 将标题转换为HTML
        markdown = markdown.replaceAll("## (.*)", "<h2>$1</h2>");

        // 将粗体转换为HTML
        markdown = markdown.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");

        // 处理无序列表
        markdown = markdown.replaceAll("\\* (.*)", "<li>$1</li>");
        markdown = markdown.replaceAll("(?m)(<li>.*</li>)", "<ul>$1</ul>");
        markdown = markdown.replaceAll("</ul>\n<ul>", "");

        // 将换行符转换为HTML的换行符
        markdown = markdown.replaceAll("\n", "<br>");

        html.append(markdown);

        return html.toString();
    }

    public static void main(String[] args) {
        String markdown = "## Naming Suggestions for \"逻辑删除\" (Logical Deletion)\n" +
                "**Suggested Name:** isDeleted\n" +
                "**Explanation:** This name clearly conveys the state of the data being logically deleted, using the \"is\" prefix for a boolean variable.\n" +
                "**Alternative Suggestions:**\n" +
                "- **deleted:** Concise and straightforward, but less specific to logical deletion.\n" +
                "- **logicallyDeleted:** More explicit but potentially verbose.\n" +
                "**Additional Comments:** \n" +
                "Since \"逻辑删除\" refers to a specific implementation detail, consider adding context to the variable name. For example, if this variable is part of a user object, you could use:\n" +
                "- **userIsDeleted:**  Clearly indicates the logical deletion status for a user object.\n" +
                "Ultimately, the best choice depends on your project's specific naming conventions and the context of the variable's usage.";

        System.out.println(convertToHtml(markdown));
    }
}
