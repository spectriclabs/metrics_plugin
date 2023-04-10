# ACECARD Metrics Plugin

The ACECARD Metrics Plugin provides:
* Webpage that provides Prometheus style metrics on the number of documents that are added to the indexes in Elasticsearch.

## Building locally
```
$ cd acecard-metrics-elasticsearch-plugin
$ gradle zip
```

## Installing
```
$ cd build/distributions
$ /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch file://$PWD/acecard-metrics-7.6.0.zip
```

## Configuration
By default the ACECARD Metrics plugin is enabled.

| Setting                                                                | Default                                        | Description
|------------------------------------------------------------------------|------------------------------------------------|------------
| acecard.metrics.enabled                                                | true                                          | enables the plugin
| acecard.metrics.output_seconds                                         | 10
| how often to write the metrics for display (in seconds)


## Build and push to repository
It is recommended you use Jenkins for pushing built plugins to the repo. 

* https://jenkins.vast-inc.com/view/acecard/job/acecard-elasticsearch-plugins-publish/

> build.sh is used to build and push this plugin to the acecard repo. When you use build.sh the plugin is built using a gradle docker image. The jdk
> version of the docker image is set in build.sh (jdk_version). A source zip will also be pushed.
```
$ ./build.sh -g
```

## Rest Interfaces

This plugin provides a rest interface for retrieving the acecard metrics.

#### /_acecard/metrics

Display the current set of Prometheus metrics for the Elasticsearch indexes