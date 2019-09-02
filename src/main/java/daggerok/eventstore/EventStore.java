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
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Collections.singletonList;

@Log4j2
@ApplicationScoped
public class EventStore {

    private Path dbBasePath;
    private ObjectMapper objectMapper;

    EventStore() {} // blah..

    @Inject
    public EventStore(Path dbBasePath, ObjectMapper objectMapper) {
        this.dbBasePath = dbBasePath;
        this.objectMapper = objectMapper;
    }

    private static final String suffix = ".db.json.log";

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
        Path logFileAbsolutePath = getDbFilePath(aggregateId);
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
    public void appendAll(DomainEvent... domainEvents) {
        for (DomainEvent domainEvent : domainEvents) {
            append(domainEvent);
        }
    }

    public void append(DomainEvent domainEvent) {
        log.debug(domainEvent);

        Path logFileAbsolutePath = getDbFilePath(domainEvent.getAggregateId());
        if (Files.notExists(logFileAbsolutePath, LinkOption.NOFOLLOW_LINKS)) {
            Try.run(() -> Files.createFile(logFileAbsolutePath))
               .getOrElseThrow(this::reThrow);
        }
        String json = Try.of(() -> objectMapper.writeValueAsString(domainEvent))
                         .getOrElseThrow(this::reThrow);
        JsonNode jsonNode = Try.of(() -> objectMapper.readTree(json))
                               .getOrElseThrow(this::reThrow);
        if (Objects.nonNull(jsonNode.get("type").asText())) {
            Try.run(() -> Files.write(logFileAbsolutePath, singletonList(json), APPEND));
        }
    }

    // - stream all absolute db paths in db folder
    // - remove if any exists
    public void cleanupAll() {
        cleanupBy(entry -> entry.toString().endsWith(suffix));
    }

    public void cleanupBy(DirectoryStream.Filter<Path> pathFilter) {
        log.debug("cleanup");
        Try.run(() -> {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(dbBasePath.toAbsolutePath(), pathFilter)) {
                for (Path path : paths) {
                    log.debug("trying remove {} file from event store", path);
                    Files.deleteIfExists(path);
                }
            }
        }).onFailure(e -> log.error(e.getLocalizedMessage(), e));
    }

    // - find all files in db folder with suffixed filter
    // - take all it's filenames
    // - remove suffix part from each filename
    // - map it into UUID
    // - collect result into list
    public Collection<UUID> findAll() {
        return findAllBy(filename -> filename.endsWith(suffix));
    }

    public Collection<UUID> findAllBy(Predicate<String> filenamePredicate) {
        String[] files = dbBasePath.toAbsolutePath().toFile().list(
                (dir, filename) -> filenamePredicate.test(filename));
        log.info("found filenames: {}", files);
        return Optional.ofNullable(files)
                       .map(Arrays::stream)
                       .orElse(Stream.empty())
                       .map(filename -> filename.replace(suffix, ""))
                       .map(UUID::fromString)
                       .collect(Collectors.toList());
    }

    /* Private API */

    private Path getDbFilePath(UUID aggregateId) {
        Objects.requireNonNull(aggregateId);
        String logFilename = String.format("%s%s", aggregateId.toString(), suffix);
        String absoluteParentPath = dbBasePath.toAbsolutePath().toString();
        return Paths.get(absoluteParentPath, logFilename);
    }

    private RuntimeException reThrow(Throwable throwable) {
        log.warn(throwable.getLocalizedMessage(), throwable);
        return new RuntimeException(throwable.getClass() + ": " + throwable.getLocalizedMessage());
    }
}
