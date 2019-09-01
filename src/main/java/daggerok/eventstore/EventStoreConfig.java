package daggerok.eventstore;

import lombok.extern.log4j.Log4j2;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@ApplicationScoped
public class EventStoreConfig {

    @Produces
    private Map<String, String> config = new ConcurrentHashMap<>();

    // Keep in mind: PostConstruct (if needed) happens earlier then ContainerInitialized event will occur, but!
    // It's Lazy, while ContainerInitialized event will be eventually produced after during application bootstrap
    // private void on(@Observes ContainerInitialized containerInitialized) {
    @PostConstruct
    public void init() {

        try (InputStream resourceAsStream = EventStoreConfig.class
                .getResourceAsStream("/META-INF/microprofile-config.properties")) {

            Properties properties = new Properties();
            properties.load(resourceAsStream);
            properties.stringPropertyNames()
                      .forEach(key -> config.put(key, properties.getProperty(key)));

        } catch (Throwable throwable) {
            log.error(throwable.getLocalizedMessage(), throwable);
            throw new RuntimeException(throwable);
        }
    }

    @Produces
    private Path dbBasePath() {
        return config.containsKey("eventStore.dbBasePath")
                ? Paths.get(config.get("eventStore.dbBasePath"))
                : Paths.get("target", "db");
    }
}
