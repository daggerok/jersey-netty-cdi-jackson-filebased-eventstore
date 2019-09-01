package daggerok.eventstore.events.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import daggerok.eventstore.events.DomainEvent;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CounterCreated implements DomainEvent {

    private final String eventName = CounterCreated.class.getSimpleName();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID aggregateId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String counterName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private ZonedDateTime at;

    @JsonCreator
    public CounterCreated(@JsonProperty("aggregateId") UUID aggregateId,
                          @JsonProperty("counterName") String counterName,
                          @JsonProperty("at") ZonedDateTime at) {

        this.aggregateId = Optional.of(aggregateId).orElse(UUID.randomUUID());
        this.counterName = Optional.ofNullable(counterName).orElse(String.format("counter-%d", System.nanoTime()));
        this.at = Optional.ofNullable(at).orElse(ZonedDateTime.now());
    }
}
