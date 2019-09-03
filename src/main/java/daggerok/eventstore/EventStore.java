package daggerok.eventstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import daggerok.domain.Counter;
import daggerok.eventstore.events.DomainEvent;
import io.vavr.Predicates;
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

/**
 * DO NOT USE IT IN PRODUCTION!
 * <p>
 * A dead simple event store:
 * target
 * |
 * +- db
 * |
 * +- 0000000-0000-0000-0000-000000000000.json.log
 * ...
 * +- 0000000-0000-0000-0000-00000000000N.json.log
 * db file per aggregate...
 * <p>
 * Snapshots: we will have file store like this:
 * target
 * |
 * +- db
 * |
 * +- 0000000-0000-0000-0000-000000000000.json.log      (operational append only log)
 * +- 0000000-0000-0000-0000-000000000000.past.json.log (past source of true batch append only log)
 * +- 0000000-0000-0000-0000-000000000000.snapshot.json (snapshot aggregate state built from past eventlog)
 * ...
 * +- 0000000-0000-0000-0000-00000000000N.json.log
 * +- 0000000-0000-0000-0000-00000000000N.past.json.log
 * +- 0000000-0000-0000-0000-00000000000N.snapshot.json
 * <p>
 * here we have 3 files for each aggregate:
 * - *.json.log is using currently for new events appending (can be empty or missing from beginning, until first event will be stored)
 * - *.snapshot.json latest created snapshot of an aggregate (can contains empty JSON object: '{}' or object like this '{"value":0, "values":[]}')
 * - *.past.json.log contains all aggregate events where used to build current snapshot (can be empty from beginning until first snapshot is built)
 * <p>
 * to implement snapshots, we can agreed on next: if snapshot has been triggered with current aggregate value:
 * - all data must be transferred from: ${UUID}.json.log into (appended): ${UUID}.past.json.log
 * - given even must be placed (replaced) into cleared ${UUID}.json.log
 * - snapshot data file must be placed (replaced) into: ${UUID}.snapshot.json
 * <p>
 * now we must finally change way of how to read from event store:
 * 1. recreate counter since last snapshot:
 * Counter(${UUID}.snapshot.json).apply(${UUID}.json.log) -> Counter State
 * 2. recreate counter from beginning of the times:
 * Counter(empty {} snapshot).apply(${UUID}.past.json.log, -> ${UUID}.json.log) -> Counter State
 */
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

    /* Public API */

    // step 1: get or create file by filename "${aggregateId}.past.json.log"
    // stream it's content...
    // step 1: get or create file by filename "${aggregateId}.json.log"
    // stream it's content too...
    // step 2: read line by line json strings from "${aggregateId}.json.log" file
    // step 3: using Stream API map them into DomainEvents
    // step 4: collect and return new CopyOnWriteArrayList with events found.
    public Collection<DomainEvent> read(UUID aggregateId) {
        log.debug(aggregateId);
        Path pastEventLog = createAndGetDbFilePath(aggregateId, ".past.json.log");
        @Cleanup Stream<String> streamPastEventLog = Try.of(() -> Files.lines(pastEventLog))
                                                        .getOrElseThrow(this::reThrow)
                                                        .filter(Objects::nonNull)
                                                        .map(String::trim)
                                                        .filter(Predicates.not(String::isEmpty))
                                                        .peek(log::info);
        Path eventLog = createAndGetDbFilePath(aggregateId, ".json.log");
        @Cleanup Stream<String> streamEventLog = Try.of(() -> Files.lines(eventLog))
                                                    .getOrElseThrow(this::reThrow)
                                                    .filter(Objects::nonNull)
                                                    .map(String::trim)
                                                    .filter(Predicates.not(String::isEmpty))
                                                    .peek(log::info);
        return Stream.concat(streamPastEventLog, streamEventLog)
                     .map(json -> Try.of(() -> objectMapper.readValue(json, DomainEvent.class))
                                     .getOrElseThrow(this::reThrow))
                     .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
    }

    // step 1: find file by filename "${domainEvent.getAggregateId()}.json.log"
    //   - if file doesn't exists -> create new one
    // step 2: convert domainEvent object into JSON string
    // step 3: if json doesn't contains type field, throw an exception
    // step 4: append JSON string into end of the "${domainEvent.getAggregateId()}.json.log" file
    public void appendAll(DomainEvent... domainEvents) {
        for (DomainEvent domainEvent : domainEvents) {
            append(domainEvent);
        }
    }

    public void append(DomainEvent domainEvent) {
        log.debug(domainEvent);
        Path eventLog = createAndGetDbFilePath(domainEvent.getAggregateId(), ".json.log");
        String json = Try.of(() -> objectMapper.writeValueAsString(domainEvent))
                         .getOrElseThrow(this::reThrow);
        JsonNode jsonNode = Try.of(() -> objectMapper.readTree(json))
                               .getOrElseThrow(this::reThrow);
        if (Objects.nonNull(jsonNode.get("type").asText())) {
            Try.run(() -> Files.write(eventLog, singletonList(json), APPEND));
        }
    }

    // - stream all absolute db paths in db folder
    // - remove if any exists
    public void cleanupAll() {
        log.debug("clearing all *.json* files...");
        cleanupBy(entry -> entry.toString().contains(".json"));
    }

    public void cleanupBy(DirectoryStream.Filter<Path> pathFilter) {
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
        return findAllBy(filename -> filename.endsWith(".past.json.log"));
    }

    public Collection<UUID> findAllBy(Predicate<String> filenamePredicate) {
        String[] files = dbBasePath.toAbsolutePath().toFile().list(
                (dir, filename) -> filenamePredicate.test(filename));
        log.info("found filenames: {}", files);
        return Optional.ofNullable(files)
                       .map(Arrays::stream)
                       .orElse(Stream.empty())
                       .map(filename -> filename.replace(".past.json.log", ""))
                       .map(UUID::fromString)
                       .collect(Collectors.toList());
    }

    /* Snapshot API */

    public void snapshot(Counter aggregate) {
        log.debug("snapshotting: {}", aggregate);
        // place all domain events out from aggregate into fresh new clean ${aggregateId}.json.log file
        appendAll(aggregate.getEventStream().toArray(new DomainEvent[0]));
        // clean aggregate.eventStream
        aggregate.getEventStream().clear();
        // serialize aggregate into json and replace with it content of ${aggregateId}.snapshot.json file
        saveSnapshot(aggregate);
        // crete ${aggregateId}.past.json.log
        Path pastEventLog = createAndGetDbFilePath(aggregate.getAggregateId(), ".past.json.log");
        // read ${aggregateId}.json.log file and append everything from it, line-by-line into ${aggregateId}.past.json.log
        Path eventLog = createAndGetDbFilePath(aggregate.getAggregateId(), ".json.log");
        List<String> jsons = Try.of(() -> Files.lines(eventLog))
                                .getOrElseThrow(this::reThrow)
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(Predicates.not(String::isEmpty))
                                .collect(Collectors.toList());
        Try.run(() -> Files.write(pastEventLog, jsons, APPEND));
        // // that just doesn't worked (truncate ${aggregateId}.json.log file) see deletion workaround...
        // Try.run(() -> Files.write(eventLog, new byte[0], StandardOpenOption.TRUNCATE_EXISTING));
        // remove ${aggregateId}.json.log file
        cleanupBy(entry -> entry.toAbsolutePath()
                                .toString()
                                .endsWith(String.format("%s.json.log", aggregate.getAggregateId())));
    }

    /* Private API */

    private void saveSnapshot(Counter aggregate) {
        Objects.requireNonNull(aggregate);
        Path snapshotFile = createAndGetDbFilePath(aggregate.getAggregateId(), ".snapshot.json");
        Try.of(() -> objectMapper.writeValueAsString(aggregate))
           .andThenTry(json -> Files.write(snapshotFile, singletonList(json)))
           .getOrElseThrow(this::reThrow);
    }

    private Path createAndGetDbFilePath(UUID aggregateId, String suffix) {
        Path dbFilePath = getDbFilePath(aggregateId, suffix);
        if (Files.notExists(dbFilePath, LinkOption.NOFOLLOW_LINKS)) {
            Try.run(() -> Files.createFile(dbFilePath))
               .getOrElseThrow(this::reThrow);
        }
        return dbFilePath;
    }

    private Path getDbFilePath(UUID aggregateId, String suffix) {
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
