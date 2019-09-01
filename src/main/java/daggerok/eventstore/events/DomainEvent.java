package daggerok.eventstore.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME/*,
        include = JsonTypeInfo.As.PROPERTY*/,
        property = "type"/*,
        visible = false*/
)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "CounterCreated", value = CounterCreated.class),
        @JsonSubTypes.Type(name = "CounterIncremented", value = CounterIncremented.class),
        @JsonSubTypes.Type(name = "CounterSuspended", value = CounterSuspended.class),
})
public interface DomainEvent {
    UUID getAggregateId();

    String getEventName();
    // String getType();
}
