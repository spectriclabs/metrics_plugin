# ACECARD Security Pack

The ACECARD security app provides:
* Provisions X-Pack users with roles based on their current CASPORT profile.
* Audit logging of search queries and realm/role queries
* Camkey management

## Clone
```
$ git@git.vast-inc.com:acecard/acecard-security-elasticsearch-plugin.git
```

## Building locally
```
$ cd acecard-security-elasticsearch-plugin
$ gradle zip
```

## Installing
```
$ cd build/distributions
$ /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch file://$PWD/acecard-security-7.6.0.zip
```

## Configuration
By default the ACECARD security plugin is disabled. It must be explicily enabled by setting `acecard.security.enabled=true` in the configuration.

| Setting                                                                | Default                                        | Description
|------------------------------------------------------------------------|------------------------------------------------|------------
| acecard.security.enabled                                               | false                                          | enables the plugin
| acecard.security.casport.url **(DYNAMIC)**                             | https://localhost/acecard-casport/lookupUser   | Url to hit to look up casport profile
| acecard.security.casport.timeout_in_seconds                            | 30                                             | Timeout for casport comms
| acecard.security.ssl_host_validation_enabled                           | true                                           | 
| acecard.security.truststore_path                                       |                                                | path to the truststore
| acecard.security.truststore_password                                   |                                                | truststore password
| acecard.security.truststore_type                                       |                                                | trustore type (ex. jks, pkcs12)
| acecard.security.keystore_path                                         |                                                | path to the keystore
| acecard.security.keystore_password                                     |                                                | keystore password
| acecard.security.keystore_type                                         |                                                | keystore type (ex. jks, pkcs12)     
| acecard.security.xproxy_chain_header **(DYNAMIC)**                     | X-ProxiedEntitiesChain                         |
| acecard.security.xproxy_chain_role_prefix **(DYNAMIC)**                | acecard-chained-                               |
| acecard.security.xproxy_accept_header **(DYNAMIC)**                    | X-ProxiedEntitiesAccepted                      | Response header for success auth
| acecard.security.xproxy_reject_header **(DYNAMIC)**                    | X-ProxiedEntitiesRejected                      | Response header for failed auth
| acecard.security.xproxy_details_header **(DYNAMIC)**                   | X-ProxiedEntitiesDetails                       | Response header with failed auth details
| acecard.security.xproxy_response.enabled **(DYNAMIC)**                  | false                                          | If enabled, xproxy headers will be included in Search responses
| acecard.security.dn_header **(DYNAMIC)**                               | casport-dn                                     |
| acecard.security.roles **(DYNAMIC)**                                   | kibana-user                                    |
| acecard.security.indexes **(DYNAMIC)**                                 | *                                              |
| acecard.security.role_namespace **(DYNAMIC)**                          | acecard-casport                                |
| acecard.security.camkeys **(DYNAMIC)**                                 | []                                             | Expected possible camkey values returned from casport
| acecard.security.auto_refresh_users_enabled                            | false                                          | If enabled, refresh the realm/role caches if configured camkeys or groups have changed.
| acecard.security.groups **(DYNAMIC)**                                  | []                                             | Expected possible group values returned from casport
| acecard.security.projects **(DYNAMIC)**                                |                                                |
| acecard.security.privileges **(DYNAMIC)**                              | read, view_index_metadata, read_cross_cluster  |
| acecard.security.cluster_privileges **(DYNAMIC)**                      | transport_client                               |
| acecard.security.role_cache_hours                                      | 24                                             |
| acecard.security.role_refresh_cache_hours **(DYNAMIC)**                | 1                                              |
| acecard.security.role_refresh_cache_enabled **(DYNAMIC)**              | true                                           |
| acecard.security.filter_camkeys_enabled **(DYNAMIC)**                  | true                                           | Controls if security will filter the camkeys returned from casport.
| acecard.security.filter_groups_enabled **(DYNAMIC)**                   | true                                           | Controls if security will filter the groups returned from casport.
| acecard.security.audit.enabled **(DYNAMIC)**                           | false                                          | Controls if audit logging is enabled
| acecard.security.audit.search.headers_logged_list **(DYNAMIC)**        |                                                | The headers that need to exist on an request for it to be logged
| acecard.security.audit.search.headers_required_list **(DYNAMIC)**      |                                                | If any headers are defined, all requests without them will be rejected
| acecard.security.audit.search.missing_user_ignored **(DYNAMIC)**       | true                                           |
| acecard.security.audit.search.system_indices_ignored **(DYNAMIC)**     | true                                           |
| acecard.security.audit.search.user_field **(DYNAMIC)**                 | casport-dn                                     |
| acecard.security.audit.search.action_events **(DYNAMIC)**              | []                                             | List of events to either ignore or include
| acecard.security.audit.search.ignore_action_events **(DYNAMIC)**       | false                                          | If false, will only audit actions in action_events. If true, will ignore action events in action_events.
| acecard.security.audit.search.ignore_users.field                       | casport-dn                                     | Header field to use for ignoring user audits
| acecard.security.audit.search.ignore_users.list                        | []                                             | Users in this list will not have their searches audited
| acecard.security.audit.write.indices **(DYNAMIC)**                     | []                                             | Indices for which writes will be audited. Values can be regex or concrete indices.
| acecard.security.audit.template_file                                   | /acecard-audit-template.json                   | JSON file that defines the audit index template
| acecard.security.audit.ilm_file                                        | /acecard-audit-ilm.json                        | JSON file defines the audit ILM policy
| acecard.security.audit.template_version                                | 3                                              | Template version
| acecard.security.audit.template_use_defaults                           | true                                           | If true, use the default audit template/
| acecard.security.native_roles.cache.timeout.minutes                    | 5                                              | Timeout value for the native roles cache
| acecard.security.native_roles.lookup.retry.count **(DYNAMIC)**         | 3                                              | Number of retries for querying all users to populate the native roles cache. 
| acecard.security.native_roles.lookup.retry.sleep.seconds **(DYNAMIC)** | 2                                              | Time in between user lookup retries
| acecard.security.dev_indices.enabled **(DYNAMIC)**                     | false                                          | Enable access to test indices per user
| acecard.security.dev_indices.prefix                                    | dev-index                                      | Prefix for the user's test indices
| acecard.security.dev_indices.template_file                             | /acecard-dev-indices-template.json             | JSON file that defines the audit index template
| acecard.security.dev_indices.ilm_file                                  | /acecard-dev-indices-ilm.json                  | JSON file defines the audit ILM policy
| acecard.security.dev_indices.template_version                          | 2                                              | Template version
| acecard.security.dev_indices.template_use_defaults                     | true                                           | If true, use the default audit template/
| acecard.security.whitelist.enabled **(DYNAMIC)**                       | false                                          | If enabled, check that chained request host is in the configured whitelist
| acecard.security.whitelist **(DYNAMIC)**                               | []                                             | Hosts that are allowed to proxy a request
| acecard.security.map_groups_to_roles.enabled **(DYNAMIC)**             | false                                          | If true, add casport groups to the list of elasticsearch roles
| acecard.security.map_groups_to_roles.mapping **(DYNAMIC)**             | []                                             | Casport groups mapped to native roles. Ex (group:roles,...): ["group1:acecard_default,reporting", "group2:read_only"]

## Build and push to repository
It is recommended you use Jenkins for pushing built plugins to the repo. 

* https://jenkins.vast-inc.com/view/acecard/job/acecard-elasticsearch-plugins-publish/

> build.sh is used to build and push this plugin to the acecard repo. When you use build.sh the plugin is built using a gradle docker image. The jdk
> version of the docker image is set in build.sh (jdk_version). A source zip will also be pushed.
```
$ ./build.sh -g
```

## Rest Interfaces

This plugin provides multiple rest interfaces for interacting with the acecard realm.

#### /_acecard/security/_clear_cache

Clear all cached acecard/native roles from the acecard realm.
                     
#### /_acecard/security/_clear_cache/{usernames}

Clear the provided user's cached acecard/native roles from the acecard realm.
