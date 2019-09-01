package daggerok.v1;

import daggerok.eventstore.EventStore;
import daggerok.eventstore.EventStoreConfig;
import daggerok.eventstore.EventStoreResource;
import daggerok.infrastructure.ErrorMapper;
import daggerok.infrastructure.HealthResource;
import io.netty.channel.Channel;
import io.vavr.collection.HashSet;
import lombok.extern.log4j.Log4j2;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.Application;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Log4j2
public class Main extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return HashSet.of(
                EventStore.class,
                EventStoreConfig.class,
                //
                ErrorMapper.class,
                EventStoreResource.class,
                HealthResource.class/*,
                com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider.class*/
        ).toJavaSet();
    }

    public static void main(String[] args) {

        Instant startAt = Instant.now();
        URI baseUri = URI.create("http://127.0.0.1:8080/");
        ResourceConfig configuration = ResourceConfig.forApplicationClass(Main.class);
        Channel server = NettyHttpContainerProvider.createHttp2Server(baseUri, configuration, null);

        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        log.info("Server started in {} sec. Use CTRL+C to quit",
                 Duration.between(startAt, Instant.now()).toMillis() / 1000.0);
    }
}
