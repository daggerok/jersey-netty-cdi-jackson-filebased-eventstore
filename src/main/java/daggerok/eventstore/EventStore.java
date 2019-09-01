package daggerok.eventstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import daggerok.eventstore.events.DomainEvent;
import io.vavr.control.Try;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Collections.singletonList;

@Log4j2
@ApplicationScoped
public class EventStore {

    @Inject
    private Path dbBasePath;

    @Inject
    private ObjectMapper objectMapper;

    // Keep in mind: PostConstruct (if needed) happens earlier then ContainerInitialized event will occur, but!
    // PostConstruct is Lazy, while ContainerInitialized event will be eventually produced during application bootstrap
    // private void on(@Observes ContainerInitialized containerInitializedEvent) {
    @PostConstruct
    public void postConstruct() {
        Path dbDir = dbBasePath.toAbsolutePath();
        if (Files.notExists(dbDir, LinkOption.NOFOLLOW_LINKS)) {
            Try.run(() -> Files.createDirectories(dbDir))
               .getOrElseThrow(this::reThrow);
        }
        log.debug("EventStore constructed.");
    }

    // step 1: find file by filename "${aggregateId}.db.json.log"
    //   - if file doesn't exists -> return empty collection\
    //   - otherwise -> step 2
    // step 2: read line by line json strings from "${aggregateId}.db.json.log" file
    // step 3: using Stream API map them into DomainEvents
    // step 4: collect and return new CopyOnWriteArrayList with events found.
    public Collection<DomainEvent> read(UUID aggregateId) {
        log.debug(aggregateId);
        Path logFileAbsolutePath = getLogFilenamePath(aggregateId);
        if (Files.notExists(logFileAbsolutePath, LinkOption.NOFOLLOW_LINKS)) return new CopyOnWriteArrayList<>();
        @Cleanup Stream<String> jsonStream = Try.of(() -> Files.lines(logFileAbsolutePath))
                                                .getOrElseThrow(this::reThrow)
                                                .peek(log::info);
        return jsonStream.map(json -> Try.of(() -> objectMapper.readValue(json, DomainEvent.class))
                                         .getOrElseThrow(this::reThrow))
                         .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
    }

    // step 1: find file by filename "${domainEvent.getAggregateId()}.db.json.log"
    //   - if file doesn't exists -> create new one
    // step 2: convert domainEvent object into JSON string
    // step 3: if json doesn't contains type field, throw an exception
    // step 4: append JSON string into end of the "${domainEvent.getAggregateId()}.db.json.log" file
    public void append(DomainEvent... domainEvents) {
        for (DomainEvent domainEvent : domainEvents) {
            log.debug(domainEvent);

            Path logFileAbsolutePath = getLogFilenamePath(domainEvent.getAggregateId());
            if (Files.notExists(logFileAbsolutePath, LinkOption.NOFOLLOW_LINKS)) {
                Try.run(() -> Files.createFile(logFileAbsolutePath))
                   .getOrElseThrow(this::reThrow);
            }
            String json = Try.of(() -> objectMapper.writeValueAsString(domainEvent))
                             .getOrElseThrow(this::reThrow);
            JsonNode jsonNode = Try.of(() -> objectMapper.readTree(json))
                                   .getOrElseThrow(this::reThrow);
            if (Objects.nonNull(jsonNode.get("type").asText())) {
                Try.run(() -> Files.write(logFileAbsolutePath, singletonList(json), UTF_8, APPEND));
            }
        }
    }

    private Path getLogFilenamePath(UUID aggregateId) {
        Objects.requireNonNull(aggregateId);
        String logFilename = String.format("%s.db.json.log", aggregateId.toString());
        String absoluteParentPath = dbBasePath.toAbsolutePath().toFile().getAbsolutePath();
        return Paths.get(absoluteParentPath, logFilename);
    }

    private RuntimeException reThrow(Throwable throwable) {
        log.warn(throwable.getLocalizedMessage(), throwable);
        return new RuntimeException(throwable.getClass() + ": " + throwable.getLocalizedMessage());
    }
}
