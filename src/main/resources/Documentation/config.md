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

- `contributors.ignore-file-suffix`

  List of file suffixes to be ignored from the analytics.
  Files matching any of the specified suffixes will not be accounted for in
  `num_files`, `num_distinct_files`, `added_lines` and `deleted_lines` fields
  nor will they be listed in the `commits.files` array field.
  This can be used to explicitly ignore binary files for which, file-based
  statistics makes little or no sense.

  Default: empty

  Example:
  ```ini
  [contributors]
    ignore-file-suffix = .dmg
    ignore-file-suffix = .ko
    ignore-file-suffix = .png
    ignore-file-suffix = .exe
  ```
- `contributors.extract-hashtags`

  when set to true, enables the extraction of hash-tags from the change commits

  Default: false

  example:
  ```ini
  [contributors]
    extract-hashtags = true
  ```