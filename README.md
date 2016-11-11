# Analytics extraction plugin

Extract commit and review data from Gerrit projects and expose aggregated metrics
over REST and SSH API.

## How to build

Clone the analytics plugin into an existing Gerrit source tree under /plugins/analytics
and then execute buck build.

Example:

```
   $ git clone https://gerrit.googlesource.com/gerrit
   $ git clone https://gerrit.googlesource.com/plugins/analytics gerrit/plugins/analytics
   $ cd gerrit
   $ buck build plugins/analytics
```

## How to install

Copy the analytics.jar generated under /buck-out/gen/plugins/analytics/analytics.jar
onto Gerrit's /plugins directory.

## How to configure

Nothing to configure, it just works.

## How to use

Adds new REST API and SSH commands to allow the extraction of repository statistics
from Gerrit repositories and changes.

## API

All the API share the same syntax and behaviour. Differently from the standard Gerrit REST
API, the JSON collections are returned as individual lines and streamed over the socket
I/O. The choice is driven by the fact that the typical consumer of these API is a BigData
batch process, typically external to Gerrit and hosted on a separate computing cluster.

A large volume of data can be potentially generated: splitting the output file into separate
lines helps the BigData processing in the splitting, shuffling and sorting phase.

### Contributors

Extract a unordered list of project contributors statistics, including the commits data
relevant for statistics purposes, such as timestamp and merge flag.

*REST*

/projects/{project-name}/analytics~contributors

*SSH*

analytics contributors {project-name}

REST Example:

```
   $ curl http://gerrit.mycompany.com/project/myproyject/analytics~contributors

   {"name":"John Doe","email":"john.doe@mycompany.com","num_commits":1,"commits":[{"sha1":"6a1f73738071e299f600017d99f7252d41b96b4b","date":"Apr 28, 2011 5:13:14 AM","merge":false}]}
   {"name":"Matt Smith","email":"matt.smith@mycompany.com","num_commits":1,"commits":[{"sha1":"54527e7e3086758a23e3b069f183db6415aca304","date":"Sep 8, 2015 3:11:23 AM","merge":true}]}
```

SSH Example:

```
   $ ssh -p 29418 admin@gerrit.mycompany.com analytics contributors

   {"name":"John Doe","email":"john.doe@mycompany.com","num_commits":1,"commits":[{"sha1":"6a1f73738071e299f600017d99f7252d41b96b4b","date":"Apr 28, 2011 5:13:14 AM","merge":false}]}
   {"name":"Matt Smith","email":"matt.smith@mycompany.com","num_commits":1,"commits":[{"sha1":"54527e7e3086758a23e3b069f183db6415aca304","date":"Sep 8, 2015 3:11:23 AM","merge":true}]}
```

