load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
  maven_jar(
      name = 'gitective-core',
      artifact = 'io.fabric8:gitective-core:0.9.18',
      sha1 = 'e9e3cd5c83da434ad64eadd08efd02a1e33fb3fb',
  )
