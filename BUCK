include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//lib/maven.defs')

gerrit_plugin(
  name = 'analytics',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: analytics',
    'Gerrit-Module: com.googlesource.gerrit.plugins.analytics.Module',
    'Gerrit-SshModule: com.googlesource.gerrit.plugins.analytics.SshModule',
    'Implementation-Title: Analytics plugin',
    'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/analytics',
  ],
  deps = [
    ':gitective-core',
  ],
)

java_test(
  name = 'analytics_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['analytics'],
  deps = GERRIT_PLUGIN_API + GERRIT_TESTS + [
    ':analytics__plugin',
  ],
)

maven_jar(
  name = 'gitective-core',
  id = 'io.fabric8:gitective-core:0.9.18-1',
  repository = MAVEN_LOCAL,
  license = 'Apache2.0',
)
