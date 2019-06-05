# Analytics extraction plugin configuration

Once installed, this plugin requires no additional configuration and can just be
 used out of the box.

You can however, tweak gerrit.config, as follows

### Commits Statistics Cache

To increase throughput and minimize resource consumption this plugin makes use
of an internal cache to store statistics associated to specific object ids.

Depending on your needs you can reserve more memory to it, set expiration
policies and decide whether you want to persist it on disk.

To achieve this, this plugin makes use of Gerrit built-in cache mechanism as
 described [here](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#cache).
As an example, you could add the following stanza to the `gerrit.config` file:

```
[cache "analytics.commits_statistics_cache"]
        memoryLimit = 100000
        diskLimit = 52428800
```

Defaults:
* `diskLimit`: disk storage for the cache is disabled
* `memoryLimit`: 100000

### Binary Files Cache

When the `ignore-binary-file` is set in the analytics configuration, the contributors endpoint will detect binary files
that are part of commits and it will exclude them from the analytics result.
Checking whether a file is binary is a quite expensive operation and thus it makes sense to cache the result of this computation.

The binary files cache fulfills precisely this purpose by associating a boolean value to the examined file-path for the requested project.

Again, To achieve this, this plugin makes use of Gerrit built-in cache mechanism as described [here](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#cache).
As an example, you could add the following configuration to the `gerrit.config` file:

```
[cache "analytics.binary_files_cache"]
        memoryLimit = 100000
        diskLimit = 52428800
```

* `diskLimit`: disk storage for the cache is disabled
* `memoryLimit`: 100000