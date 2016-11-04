include_defs('//bucklets/gerrit_plugin.bucklet')

gerrit_plugin(
  name = 'analytics',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: analytics',
    'Implementation-Title: Analytics plugin',
    'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/analytics',
  ],
  deps = [
  ],
)

java_test(
  name = 'analytics_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['secure-config'],
  deps = GERRIT_PLUGIN_API + GERRIT_TESTS + [
    ':analytics__plugin',
  ],
)
