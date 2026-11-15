package com.hmydk.aigit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmydk.aigit.context.AIPromptBuilder;
import com.hmydk.aigit.context.CommitContext;
import com.hmydk.aigit.context.FileChange;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless IntelliJ Platform integration test for the prompt input of issue #84.
 *
 * <p>It boots the real IntelliJ Platform test application and runs the same prompt resolution the
 * commit action uses ({@code CommitMessageService.resolvePrompt}), including {@code ContextAnalyzer}
 * and the structured JSON serialization. The change under test is a 2000-line file with a single
 * inserted line: the old index-based diff turned the remaining 1500 lines into fake changes, which
 * pushed the prompt over the 20000-character limit and forced the model to work from a truncated
 * diff. No provider is called and no network is needed, so this test intentionally runs without a
 * mock HTTP endpoint.</p>
 */
public class PromptResolutionIntegrationTest extends BasePlatformTestCase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int PROMPT_LIMIT = CommitMessageService.MAX_PROMPT_LENGTH;

    public void testLargeFileWithOneInsertionStaysInFullMode() throws Exception {
        List<String> original = numberedLines(1, 2000);
        List<String> inserted = new ArrayList<>(original);
        inserted.add(500, "inserted by developer");

        CommitContext context = contextFor("/repo/src/Example.java",
                String.join("\n", original), String.join("\n", inserted));

        CommitMessageService.ResolvedPrompt resolved =
                CommitMessageService.resolvePrompt(context, aiInput -> aiInput, PROMPT_LIMIT);

        assertEquals(AIPromptBuilder.InputMode.FULL, resolved.mode());
        assertFalse("prompt should not be truncated: " + resolved.prompt().length() + " chars",
                resolved.prompt().length() > PROMPT_LIMIT);
        assertTrue("prompt must contain the real change:\n" + resolved.prompt(),
                resolved.prompt().contains("+inserted by developer"));
        assertFalse("prompt must not contain untouched lines:\n" + resolved.prompt(),
                resolved.prompt().contains("line-1500"));

        JsonNode json = extractJson(resolved.aiInput());
        assertEquals("full", json.path("input_mode").asText());
        assertFalse(json.path("truncated").asBoolean());
    }

    public void testCrlfToLfConversionIsNotReportedAsWholeFileChange() throws Exception {
        List<String> original = numberedLines(1, 400);
        List<String> modified = new ArrayList<>(original);
        modified.set(199, "line-200 changed");

        CommitContext context = contextFor("/repo/src/Windows.java",
                String.join("\r\n", original), String.join("\n", modified));

        CommitMessageService.ResolvedPrompt resolved =
                CommitMessageService.resolvePrompt(context, aiInput -> aiInput, PROMPT_LIMIT);

        assertEquals(AIPromptBuilder.InputMode.FULL, resolved.mode());
        assertTrue(resolved.prompt().contains("-line-200"));
        assertTrue(resolved.prompt().contains("+line-200 changed"));
        assertFalse("line endings must not become fake changes:\n" + resolved.prompt(),
                resolved.prompt().contains("line-350"));
    }

    /**
     * Issue #98 was reported as "the generated message does not describe the real change": the
     * answer was {@code refactor(core)} for a small functional edit. The type came from
     * {@code analysis.pattern}, which used to label every change set whose files were mostly
     * modified as {@code REFACTORING}. This test runs the same prompt resolution the action uses
     * and checks that a small two-file edit is not presented to the model as a refactoring.
     */
    public void testSmallFunctionalEditIsNotPresentedAsRefactoring() throws Exception {
        FilePath controller = filePath("/repo/app/api/v1/controller/AccountController.php");
        FilePath service = filePath("/repo/app/api/v1/service/rental/AccountSearchService.php");

        List<FileChange> changes = FileChange.fromGitChanges(List.of(
                new Change(revision(controller, "before controller"),
                        revision(controller, "before controller\nadded query filter")),
                new Change(revision(service, "before service\nremoved line"),
                        revision(service, "before service\nadded line 1\nadded line 2"))
        ), List.of());
        CommitContext context = CommitContext.create(getProject(), changes);

        CommitMessageService.ResolvedPrompt resolved =
                CommitMessageService.resolvePrompt(context, aiInput -> aiInput, PROMPT_LIMIT);
        JsonNode json = extractJson(resolved.aiInput());

        assertEquals("full", json.path("input_mode").asText());
        assertFalse("a 5-line functional edit must not be reported as a refactoring: " + resolved.aiInput(),
                "pattern".equals(json.path("analysis").path("pattern").asText()));
        assertFalse("the prompt must not tell the model to trust the heuristic blindly:\n" + resolved.prompt(),
                resolved.prompt().contains("Leverage analysis.pattern"));
        assertTrue("the prompt must tell the model to verify the heuristics against the diff",
                resolved.prompt().contains("verify"));
    }

    private CommitContext contextFor(String path, String beforeContent, String afterContent) {
        FilePath filePath = filePath(path);
        Change change = new Change(revision(filePath, beforeContent), revision(filePath, afterContent));
        List<FileChange> changes = FileChange.fromGitChanges(List.of(change), List.of());
        assertEquals(1, changes.size());
        return CommitContext.create(getProject(), changes);
    }

    private static List<String> numberedLines(int fromInclusive, int toInclusive) {
        List<String> lines = new ArrayList<>();
        for (int i = fromInclusive; i <= toInclusive; i++) {
            lines.add("line-" + i);
        }
        return lines;
    }

    private JsonNode extractJson(String aiInput) throws Exception {
        int start = aiInput.indexOf('{');
        int end = aiInput.lastIndexOf('}');
        assertTrue(start >= 0 && end > start);
        return OBJECT_MAPPER.readTree(aiInput.substring(start, end + 1));
    }

    private ContentRevision revision(FilePath path, String content) {
        return proxy(ContentRevision.class, (method, args) -> switch (method.getName()) {
            case "getFile" -> path;
            case "getContent" -> content;
            default -> defaultValue(method.getReturnType());
        });
    }

    private FilePath filePath(String path) {
        return proxy(FilePath.class, (method, args) -> switch (method.getName()) {
            case "getPath", "getPresentableUrl" -> path;
            case "getName" -> path.substring(path.lastIndexOf('/') + 1);
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + " test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return invocation.invoke(method, args);
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(Method method, Object[] args);
    }
}
