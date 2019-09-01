package daggerok.eventstore.events.v2;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "CounterCreatedV2", value = CounterCreated.class),
        @JsonSubTypes.Type(name = "CounterIncrementedV2", value = CounterIncremented.class),
        @JsonSubTypes.Type(name = "CounterSuspendedV2", value = CounterSuspended.class),
})
public interface DomainEvent {
    UUID getAggregateId();

    String getType();
}
