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

- `contributors.ignore-binary-files`

  boolean value to indicate whether binary files should be ignored from the analytics.
  This means that binary files will not be accounted for in `num_files`,
  `num_distinct_files`, `added_lines` and `deleted_lines` fields and they will not
  be listed in the `commits.files` field either.

  Default: false

  example:
  ```ini
  [contributors]
    ignore-binary-files = true
  ```