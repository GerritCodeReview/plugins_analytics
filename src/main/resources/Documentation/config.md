# Analytics plugin configuration

The configuration is located on `$GERRIT_SITE/etc/analytics.config` and allows
to customize the default parameters for the analytics extraction.

## Parameters

- `contributors.botlike-filename-regexp`

  List of regexps that identify a bot-like commit, commits that modify only
  files whose name is a match will be flagged as bot-like.

  Default: empty

  Example:
  ```ini
  [contributors]
    botlike-filename-regexp = .+\.xml
    botlike-filename-regexp = .+\.bzl
    botlike-filename-regexp = BUILD
    botlike-filename-regexp = WORKSPACE
  ```