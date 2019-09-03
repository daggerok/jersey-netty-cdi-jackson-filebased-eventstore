package daggerok.eventstore;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import daggerok.domain.Counter;
import daggerok.eventstore.events.CounterCreated;
import daggerok.eventstore.events.CounterIncremented;
import daggerok.eventstore.events.CounterSuspended;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventStoreTest {

    private Path dbPath = Paths.get("target", "test-db-" + System.currentTimeMillis());

    private ObjectMapper objectMapper = JsonMapper.builder()
                                                  .addModules(new JavaTimeModule())
                                                  .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                                                  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                                  .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                                                  .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                                  .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                                  .build();

    private EventStore eventStore = new EventStore(dbPath, objectMapper);

    @BeforeEach
    void setUp() {
        eventStore.postConstruct();
        // eventStore.cleanupAll();
    }

    @Test
    void should_create_snapshot() {
        // given
        Counter counter = new Counter();
        //
        UUID aggregateId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        counter.create(aggregateId, "test");

        // when
        counter.increment("max", 1L);
        counter.increment("max", 2L);
        System.out.println(counter);
        eventStore.snapshot(counter);
        System.out.println(counter);
        //
        counter.increment("n0b0dy", 1L);
        System.out.println(counter);
        eventStore.snapshot(counter);
        System.out.println(counter);

        // then
        System.out.println(counter);
        counter.suspend("me", "just because I can!");
        eventStore.snapshot(counter);
        System.out.println(counter);
        //
        eventStore.snapshot(counter);
    }

    @Test
    void should_cleanup() {
        // given
        UUID aggregateId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        eventStore.appendAll(
                new CounterCreated(aggregateId),
                new CounterIncremented(aggregateId),
                new CounterIncremented(aggregateId),
                new CounterIncremented(aggregateId),
                new CounterSuspended(aggregateId)
        );

        // when
        eventStore.cleanupAll();

        // then
        assertThat(eventStore.read(aggregateId)).isNotNull()
                                                .isEmpty();
    }
}
