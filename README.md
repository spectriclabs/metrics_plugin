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
| acecard.metrics.enabled                                                | false                                          | enables the plugin
| acecard.metrics.output_seconds (DYNAMIC)                               | 10
| how often to write the metrics for display (in seconds)
| acecard.metrics.indices (DYNAMIC)                                		 | []
| list of what indices should have metric listeners added to it

## Rest Interfaces

This plugin provides a rest interface for retrieving the acecard metrics.

#### /_acecard/metrics

Display the current set of Prometheus metrics for the Elasticsearch indexes