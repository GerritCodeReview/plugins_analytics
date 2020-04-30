# Caveats

* Extracting branches and hashtags is a really expensive operation. At the moment the plugin
works for "small" repositories (~50K commits). With bigger repositories the plugin might cause OOM.

* It is not possible to extract data about a single commit, but only aggregations of them. 