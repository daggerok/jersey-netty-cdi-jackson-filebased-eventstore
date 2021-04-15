# jersey-netty-cdi-jackson-file-eventstore [![Build Status](https://travis-ci.org/daggerok/jersey-netty-cdi-jackson-file-eventstore.svg?branch=master)](https://travis-ci.org/daggerok/jersey-netty-cdi-jackson-file-eventstore)
Building File based event-store with Jackson JSON Serialisation / Deserialization, Jersey REST API uses Netty runtime and Weld CDI

```bash
git clone --depth=1 https://github.com/daggerok/jersey-netty-cdi-jackson-file-eventstore.git app
cd app/

./mvnw package ; java -jar target/*-all.jar

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

_fat jar_

```bash
./mvnw clean ; ./mvnw ; java -jar ./target/*-all.jar
```

build _docs_ on my macOS:

```bash
brew reinstall libffi
export LDFLAGS="-L/usr/local/opt/libffi/lib"
export CPPFLAGS="-I/usr/local/opt/libffi/include"
export PKG_CONFIG_PATH="/usr/local/opt/libffi/lib/pkgconfig"
export PATH=$HOME/.local/share/gem/ruby/3.0.0/bin:$PATH
gem install --user-install bundler jekyll just-the-docs

./mvnw -Dbundle=`which bundle` exec:exec
#./mvnw -Dbundle=`which bundle` -P jekyll-serve

./mvnw -Dbundle=`which bundle` -P gem-install && \
  ./mvnw -Dbundle=`which bundle` -P bundle-update-jekyll && \
  ./mvnw -Dbundle=`which bundle` -P bundle-install && \
  ./mvnw -Dbundle=`which bundle` -P jekyll-search && \
  ./mvnw -Dbundle=`which bundle` -P jekyll-build
```

NOTE: _This project has been based on [GitHub: daggerok/main-starter (branch: maven-java)](https://github.com/daggerok/main-starter/tree/maven-java)_

links:

* [Overriding Jackson ObjectMapper Provider in Jersey](https://stackoverflow.com/a/5234682/1490636)
* [spotbugs plugin](https://spotbugs.readthedocs.io/en/stable/)
