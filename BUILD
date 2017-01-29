load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)

gerrit_plugin(
    name = "analytics",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    manifest_entries = [
        "Gerrit-PluginName: analytics",
        "Gerrit-Module: com.googlesource.gerrit.plugins.analytics.Module",
        "Gerrit-SshModule: com.googlesource.gerrit.plugins.analytics.SshModule",
        "Implementation-Title: Analytics plugin",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/analytics",
    ],
    deps = [
        "@gitective-core//jar",
    ],
)

junit_tests(
    name = "analytics_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["analytics"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":analytics__plugin",
    ],
)
