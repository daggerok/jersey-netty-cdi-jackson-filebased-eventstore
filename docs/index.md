---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: home
---

# Jersey Netty CDI Jackson File EventStore [![Build Status](https://travis-ci.org/daggerok/jersey-netty-cdi-jackson-file-eventstore.svg?branch=master)](https://travis-ci.org/daggerok/jersey-netty-cdi-jackson-file-eventstore)
Building File based event-store with Jackson JSON Serialisation / Deserialization, Jersey REST API uses Netty runtime and Weld CDI

_clone repo_

```bash
git clone --depth=1 https://github.com/daggerok/jersey-netty-cdi-jackson-file-eventstore.git app
cd app/
```


_run fat jar_

```bash
./mvnw package ; java -jar target/*-all.jar
```

_test event-stpre_

```bash
echo '[
  {
    "type":"CounterCreated",
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "counterName": "hello 1"
  },
  {
    "type":"CounterIncremented",
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "by": "max",
    "withValue": 2
  },
  {
    "type":"CounterIncremented",
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "by": "max",
    "withValue": 3
  },
  {
    "type":"CounterIncremented",
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "by": "max"
  },
  {
    "type":"CounterSuspended",
    "aggregateId": "00000000-0000-0000-0000-000000000000",
    "reason": "because!"
  }
]' | http post :8080/events/collection

http :8080/events/00000000-0000-0000-0000-000000000000 Accept:application/json
http :8080/events/00000000-0000-0000-0000-000000000001/collection Accept:application/json
```


NOTE: _This project has been based on [GitHub: daggerok/main-starter (branch: maven-java)](https://github.com/daggerok/main-starter/tree/maven-java)_

links:

* [Overriding Jackson ObjectMapper Provider in Jersey](https://stackoverflow.com/a/5234682/1490636)
* [spotbugs plugin](https://spotbugs.readthedocs.io/en/stable/)

_quick project docs jekyll guide_

```bash
cd docs
bundle
bundle exec just-the-docs rake search:init
bundle exec jekyll serve
bundle exec jekyll build
mv -v _site
```
