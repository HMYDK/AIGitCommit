# IDE Compatibility

AI Git Commit compiles against IntelliJ IDEA Community 2024.1, the oldest supported platform.
The plugin descriptor intentionally has no `until-build`, so a new IDE release is not rejected
only because its build number changed.

This does not mean future compatibility is assumed. Compatibility is checked in three profiles:

| Profile | IDE | Purpose |
| --- | --- | --- |
| `minimum` | IntelliJ IDEA Community 2024.1 | Prevent use of APIs unavailable to existing users |
| `stable` | Latest stable WebStorm | Detect current product integration problems |
| `eap` | Latest WebStorm EAP | Find breaking IDE changes before release |

Run a profile locally with:

```bash
./gradlew verifyPlugin -PcompatibilityProfile=minimum
./gradlew verifyPlugin -PcompatibilityProfile=stable
./gradlew verifyPlugin -PcompatibilityProfile=eap
```

The scheduled `IDE Compatibility` workflow opens one tracking issue if a weekly verification
fails. It will not create duplicate open issues with the same title.

## Version-sensitive VCS APIs

JetBrains does not currently expose a stable public replacement for every partial-commit and
unified-diff operation used by the plugin. Those calls must stay inside
`com.hmydk.aigit.compat.DefaultIdeVcsAdapter`.

`verifyPublicApiBoundary` fails the build if one of the guarded implementation APIs is imported
elsewhere. If a future IDE changes an implementation API, update the adapter instead of adding
version checks throughout the application.

When selected-hunk extraction fails, generation stops with a compatibility error. Falling back to
the whole working-tree revision is forbidden because it can send unchecked code to the model.

## Release checklist

1. Run `./gradlew clean check buildPlugin`.
2. Run all three compatibility profiles.
3. Smoke-test tracked, untracked, deleted, moved, and partially selected changes in WebStorm.
4. Inspect the packaged `META-INF/plugin.xml` and confirm that it has `since-build="241"` and no
   `until-build`.
5. Review Plugin Verifier reports before considering a Marketplace upload.

Marketplace publishing is disabled by default. Even an explicit `publishPlugin` invocation is
skipped unless the separately approved release command includes:

```bash
-PallowMarketplacePublish=true
```
