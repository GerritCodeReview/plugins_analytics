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