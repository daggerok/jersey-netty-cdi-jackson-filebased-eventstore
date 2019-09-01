package daggerok.v3;

import io.netty.channel.Channel;
import lombok.extern.log4j.Log4j2;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Log4j2
public class Main extends ResourceConfig {

    public Main() {
        packages(true, Main.class.getPackage().getName());
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
