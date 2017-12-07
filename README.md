# Analytics extraction plugin

Extract commit and review data from Gerrit projects and expose aggregated metrics
over REST and SSH API.

## How to build

To build the analytics plugin you need to have SBT 0.13.x or later installed.
If you have a Linux operating system, see the
[Installing SBT on Linux instructions](http://www.scala-sbt.org/0.13/docs/Installing-sbt-on-Linux.html)

Clone the analytics plugin and execute ```sbt assembly```.

Example:

```
   $ git clone https://gerrit.googlesource.com/plugins/analytics
   $ cd analytics && sbt assembly
```

The plugin jar file is created under ```target/scala-2.11/analytics.jar```

## How to install

Copy the analytics.jar generated onto the Gerrit's /plugins directory.

## How to configure

Nothing to configure, it just works.

## How to use

Adds new REST API and SSH commands to allow the extraction of repository
statistics from Gerrit repositories and changes.

## API

All the API share the same syntax and behaviour. Differently from the standard
Gerrit REST API, the JSON collections are returned as individual lines and
streamed over the socket I/O. The choice is driven by the fact that the typical
consumer of these API is a BigData batch process, typically external to Gerrit
and hosted on a separate computing cluster.

A large volume of data can be potentially generated: splitting the output file
into separate lines helps the BigData processing in the splitting, shuffling and
sorting phase.

### Contributors

Extract a unordered list of project contributors statistics, including the
commits data relevant for statistics purposes, such as number of involved files,
number of added/deleted lines, timestamp and merge flag.


*REST*

/projects/{project-name}/analytics~contributors[?since=2006-01-02[15:04:05[.890][-0700]]][&until=2018-01-02[18:01:03[.333][-0700]]]

*SSH*

analytics contributors {project-name} [--since 2006-01-02[15:04:05[.890][-0700]]] [--until 2018-01-02[18:01:03[.333][-0700]]]

### Parameters

- --since --after -b Starting timestamp to consider
- --until --before -e Ending timestamp (excluded) to consider
- --aggregate -g With values email, email_year, email_month, email_day, 
email_hour to aggregate for specified temporal period defaults to email (do 
not aggregate)

NOTE: Timestamp format is consistent with Gerrit's query syntax, see /Documentation/user-search.html for details.

REST Example:

```
   $ curl http://gerrit.mycompany
   .com/projects/myproject/analytics~contributors?aggregate=email_month
   {"year":2017,"month":1,"name":"John Doe","email":"john.doe@mycompany.com",
   "num_commits":1, "num_files":4,"added_lines":9,"deleted_lines":1, "commits":[{"sha1":"6a1f73738071e299f600017d99f7252d41b96b4b","date":"Apr 28, 2011 5:13:14 AM","merge":false}]}
   {"year":2017,"month":1,"name":"Matt Smith","email":"matt.smith@mycompany.com","num_commits":1, "num_files":1,"added_lines":90,"deleted_lines":10,"commits":[{"sha1":"54527e7e3086758a23e3b069f183db6415aca304","date":"Sep 8, 2015 3:11:23 AM","merge":true}]}
```

SSH Example:

```
   $ ssh -p 29418 admin@gerrit.mycompany.com analytics contributors myproject
    --since 2017-08-01 --until 2017-12-31 --aggregate email_hour
   {"year":2017,"month":1,"day":1,"hour":12,"name":"John Doe","email":"john
   .doe@mycompany
   .com","num_commits":1, "num_files":4,"added_lines":9,"deleted_lines":1, "commits":[{"sha1":"6a1f73738071e299f600017d99f7252d41b96b4b","date":"Apr 28, 2011 5:13:14 AM","merge":false}]}
   {"year":2017,"month":1,"day":1,"hour":13,"name":"Matt Smith","email":"matt
   .smith@mycompany.com","num_commits":1, "num_files":1,"added_lines":90,"deleted_lines":10,"commits":[{"sha1":"54527e7e3086758a23e3b069f183db6415aca304","date":"Sep 8, 2015 3:11:23 AM","merge":true}]}
```

