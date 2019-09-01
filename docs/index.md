---
layout: home
title:  Jersey Netty CDI Jackson File EventStore
date:   2019-09-01 05:11:51 +0300
---
# Jersey Netty CDI Jackson File EventStore [![Build Status](https://travis-ci.org/daggerok/jersey-netty-cdi-jackson-file-eventstore.svg?branch=master)](https://travis-ci.org/daggerok/jersey-netty-cdi-jackson-file-eventstore)
Building File based event-store with Jackson JSON Serialisation / Deserialization, Jersey REST API uses Netty runtime and Weld CDI

## Jersey (Netty) and CDI

tiny, fast and dead-simple!

_pom.xml_

```xml
  <packaging>jar</packaging>

  <properties>
    <mainClass>daggerok.Main</mainClass>

    <jersey.version>2.29</jersey.version>
    <jandex.version>2.1.1.Final</jandex.version>

    <jandex-maven-plugin.version>1.0.6</jandex-maven-plugin.version>
    <capsule-maven-plugin.version>1.5.1</capsule-maven-plugin.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.glassfish.jersey</groupId>
        <artifactId>jersey-bom</artifactId>
        <version>${jersey.version}</version>
        <scope>import</scope>
        <type>pom</type>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.jboss</groupId>
      <artifactId>jandex</artifactId>
      <version>${jandex.version}</version>
    </dependency>
    <dependency>
      <groupId>org.glassfish.jersey.inject</groupId>
      <artifactId>jersey-cdi2-se</artifactId>
    </dependency>
    <dependency>
      <groupId>org.glassfish.jersey.containers</groupId>
      <artifactId>jersey-container-netty-http</artifactId>
      <version>${jersey.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- CDI index -->
      <plugin>
        <groupId>org.jboss.jandex</groupId>
        <artifactId>jandex-maven-plugin</artifactId>
        <version>${jandex-maven-plugin.version}</version>
        <executions>
          <execution>
            <id>make-index</id>
            <goals>
              <goal>jandex</goal>
            </goals>
            <phase>process-classes</phase>
          </execution>
        </executions>
      </plugin>

      <!-- fat jar -->
      <plugin>
        <groupId>com.github.chrisdchristo</groupId>
        <artifactId>capsule-maven-plugin</artifactId>
        <version>${capsule-maven-plugin.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>build</goal>
            </goals>
            <configuration>
              <fileDesc>-all</fileDesc>
              <appClass>${mainClass}</appClass>
              <type>fat</type>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

### entry point

_Main.java_ 

```java
@Log4j2
public class Main extends ResourceConfig {

    public Main() {
        packages(true, Main.class.getPackage().getName());
    }

    public static void main(String[] args) {

        Channel server = NettyHttpContainerProvider.createHttp2Server(
                URI.create("http://127.0.0.1:8080/"),
                ResourceConfig.forApplicationClass(Main.class),
                null);

        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }
}
```

### fallback

_ErrorMapper.java_

```java
@Log4j2
@Provider
@RequestScoped
public class ErrorMapper implements ExceptionMapper<Exception> {

    @Context
    UriInfo uriInfo;

    @Context
    Request request;

    @Override
    public Response toResponse(Exception exception) {
        log.warn("{} {}", request.getMethod(), exception.getLocalizedMessage(), exception);

        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(Json.createObjectBuilder()
                                   .add("error", Optional.ofNullable(exception.getLocalizedMessage())
                                                         .orElse("empty"))
                                   .add("_links", Json.createObjectBuilder()
                                                      .add("_self", uriInfo.getAbsolutePath()
                                                                           .toString())
                                                      .build())
                                   .build())
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}
```

_HealthResource.java_ + some fallback also:

```java
@RequestScoped
@Consumes(WILDCARD)
@Produces(APPLICATION_JSON)
public class HealthResource {

    private void on(@Observes ContainerInitialized containerInitializedEvent) {
        log.info(containerInitializedEvent);
    }

    @GET
    @Path("health")
    public JsonObject health() {
        log.info("health");
        return Json.createObjectBuilder()
                   .add("status", "UP")
                   .build();
    }

    @GET
    @Path("{path:(.*)?}")
    public JsonObject getAny(@PathParam("path") String path) {
        log.info("unexpected GET");
        return info(path);
    }

    private JsonObject info(String path) {
        return Json.createObjectBuilder()
                   .add("path", "" + path)
                   .build();
    }
}
```

## Jackson JSON support

maven dependencies in _pom.xml_ file

```xml
  <properties>
    <jersey.version>2.29</jersey.version>
    <jackson.version>2.10.0.pr2</jackson.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.glassfish.jersey</groupId>
        <artifactId>jersey-bom</artifactId>
        <version>${jersey.version}</version>
        <scope>import</scope>
        <type>pom</type>
      </dependency>
      <dependency>
        <groupId>com.fasterxml.jackson</groupId>
        <artifactId>jackson-bom</artifactId>
        <version>${jackson.version}</version>
        <scope>import</scope>
        <type>pom</type>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <!-- Jackson Jersey support -->
    <dependency>
      <groupId>org.glassfish.jersey.containers</groupId>
      <artifactId>jersey-container-netty-http</artifactId>
    </dependency>
    <!--
      1. Serialization / Deserialization of abstract types (such as DomainEvent)
         by using Jackson type feature
      2. REST API: Input List of DomainEvents as Request Body
    -->
    <dependency>
      <groupId>org.glassfish.jersey.media</groupId>
      <artifactId>jersey-media-json-jackson</artifactId>
    </dependency>
    <!-- REST API: javax.json.Json / javax.json.JsonObject -->
    <dependency>
      <groupId>org.glassfish.jersey.media</groupId>
      <artifactId>jersey-media-json-processing</artifactId>
    </dependency>
    <!-- Jackson ObjectMapper -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <!-- ZonedDateTime Format and Serialization / Deserialization -->
    <dependency>
      <groupId>com.fasterxml.jackson.datatype</groupId>
      <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
  </dependencies>
```

In order to let Jackson properly understand how to deserialize
json into abstract type, we have to configure its abstract
type mappings (see `@@JsonSubTypes` annotations):

_DomainEvent.java_

```java
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "CounterCreated", value = CounterCreated.class),
        @JsonSubTypes.Type(name = "CounterIncremented", value = CounterIncremented.class),
        @JsonSubTypes.Type(name = "CounterSuspended", value = CounterSuspended.class),
})
public interface DomainEvent {
    UUID getAggregateId();
}
```

All events are ValueObjects and can be created only via single constructor.
In order to help Jackson properly understand how all these events can be
deserialized into Java Object, we have to setup it's `@JsonCreator` 
together with `@JsonProperty` pointing to concrete object field...

Also, we don't wanna show any empty or null fields in case they are has
not been specified, so we are using `@JsonInclude` annotations...

By design, all our event should should be relative to concrete aggregate,
so they should have aggregateId as well:

```java
@Data
public class CounterCreated implements DomainEvent {

    private final String eventName = CounterCreated.class.getSimpleName();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final UUID aggregateId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String counterName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final ZonedDateTime at;

    @JsonCreator
    public CounterCreated(@JsonProperty("aggregateId") UUID aggregateId,
                          @JsonProperty("counterName") String counterName,
                          @JsonProperty("at") ZonedDateTime at) {

        this.aggregateId = Optional.of(aggregateId).orElse(UUID.randomUUID());
        this.counterName = Optional.ofNullable(counterName).orElse(String.format("counter-%d", System.nanoTime()));
        this.at = Optional.ofNullable(at).orElse(ZonedDateTime.now());
    }
}
```

### ZonedDateTime

In order to have properly consumable format of `java.timr.ZonedDateTime` type,
we have to configure Jackson `ObjectMapper` to be used in our serialization
process in our app:

_JacksonConfig.java_

```java
@ApplicationScoped
public class JacksonConfig {

    @Produces
    private ObjectMapper objectMapper = JsonMapper.builder()
                                                  .addModules(new JavaTimeModule())
                                                  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                                  .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                                                  .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                                  .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                                  .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                                                  .build();
}
```

But we have to also care about Jersey!
So we should properly configure Jersey
ObjectMapper `ContextResolver` via provider

_JacksonProvider_

```java
@Provider
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class JacksonProvider implements ContextResolver<ObjectMapper> {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
```

Because we previously configure `ObjectMapper` in
`JacksonConfig.java` file we can (and should) re-use it!
