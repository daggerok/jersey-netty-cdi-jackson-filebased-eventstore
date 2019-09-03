package daggerok.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import daggerok.eventstore.events.CounterCreated;
import daggerok.eventstore.events.CounterIncremented;
import daggerok.eventstore.events.CounterSuspended;
import daggerok.eventstore.events.DomainEvent;
import io.vavr.API;
import io.vavr.collection.List;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;

@Getter
@Log4j2
@ToString
public class Counter implements Function<DomainEvent, Counter> {

    @JsonIgnore
    private final Collection<DomainEvent> eventStream = new CopyOnWriteArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID aggregateId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private ZonedDateTime createdAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private ZonedDateTime modifiedAt;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long counter;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private boolean suspended;

    public Counter() {
        this(null, null, null, null, null, true);
    }

    public Counter(UUID aggregateId,
                   ZonedDateTime createdAt,
                   ZonedDateTime modifiedAt,
                   String name,
                   Long counter,
                   boolean suspended) {

        this.aggregateId = aggregateId;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.name = name;
        this.counter = counter;
        this.suspended = suspended;
    }

    /* commands */

    public void create(UUID id, String counterName) {
        if (Objects.nonNull(this.aggregateId))
            throw new IllegalStateException("current aggregate already initialized with some aggregateId");
        if (Objects.nonNull(this.name))
            throw new IllegalStateException("current aggregate already initialized with some name");
        if (Objects.isNull(id)) throw new IllegalStateException("id may not be null");
        if (Objects.isNull(counterName)) throw new IllegalStateException("counterName may not be null");
        on(new CounterCreated(id, counterName, ZonedDateTime.now()));
    }

    public void increment(String byWhom, Long withValue) {
        if (Objects.isNull(this.aggregateId)) throw new IllegalStateException("counter has not been created.");
        if (withValue < 1) throw new IllegalStateException("counter can be incremented only with positive numbers.");
        on(new CounterIncremented(aggregateId, byWhom, withValue, ZonedDateTime.now()));
    }

    public void suspend(String byWhom, String reason) {
        if (Objects.isNull(this.aggregateId))
            throw new IllegalStateException("counter has not been created.");
        on(new CounterSuspended(aggregateId, byWhom, reason, ZonedDateTime.now()));
    }

    /* event sourcing */

    public static Counter rebuild(Counter snapshot, Collection<DomainEvent> domainEvents) {
        Counter counter = List.ofAll(domainEvents)
                              .foldLeft(snapshot, Counter::apply);
        counter.eventStream.clear();
        return counter;
    }

    @Override
    public Counter apply(DomainEvent domainEvent) {
        return API.Match(domainEvent).of(
                Case($(instanceOf(CounterCreated.class)), this::on),
                Case($(instanceOf(CounterIncremented.class)), this::on),
                Case($(instanceOf(CounterSuspended.class)), this::on),
                Case($(), this::onFallback)
        );
    }

    /* events */

    private Counter on(CounterCreated event) {
        eventStream.add(event);
        aggregateId = event.getAggregateId();
        createdAt = modifiedAt = event.getAt();
        name = event.getCounterName();
        counter = 0L;
        suspended = false;
        return this;
    }

    private Counter on(CounterIncremented event) {
        eventStream.add(event);
        counter += event.getWithValue();
        modifiedAt = event.getAt();
        log.debug("{} counter incremented by {} with {}", name, event.getBy(), event.getWithValue());
        return this;
    }

    private Counter on(CounterSuspended event) {
        eventStream.add(event);
        suspended = true;
        modifiedAt = event.getAt();
        log.debug("{} counter suspended by {} with reason: {}", name, event.getBy(), event.getReason());
        return this;
    }

    private <EVENT extends DomainEvent> Counter onFallback(EVENT event) {
        log.warn("unexpected event occurred: {}", event);
        return this;
    }
}
