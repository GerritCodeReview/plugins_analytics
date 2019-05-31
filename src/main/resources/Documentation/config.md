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
    botlike-filename-regexp = .+\\.xml
    botlike-filename-regexp = .+\\.bzl
    botlike-filename-regexp = BUILD
    botlike-filename-regexp = WORKSPACE
  ```

  Keep in mind that plugin configurations are written in [git-config style syntax](https://git-scm.com/docs/git-config#_syntax),
  so you should be escaping regular expressions accordingly.

- `contributors.extract-issues`

  when set to true, enables the extraction of issues from commentLink

  Default: false

  example:
  ```ini
  [contributors]
    extract-issues = true
  ```