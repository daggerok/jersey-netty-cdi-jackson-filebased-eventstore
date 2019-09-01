package daggerok.eventstore.events.v1;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import daggerok.eventstore.events.CounterCreated;
import daggerok.eventstore.events.CounterIncremented;
import daggerok.eventstore.events.CounterSuspended;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
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
