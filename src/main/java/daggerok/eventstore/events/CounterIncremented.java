package daggerok.eventstore.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Data
public class CounterIncremented implements DomainEvent {

    private final String eventName = CounterIncremented.class.getSimpleName();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final UUID aggregateId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String by;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Long withValue;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final ZonedDateTime at;

    @JsonCreator
    public CounterIncremented(@JsonProperty("aggregateId") UUID aggregateId,
                              @JsonProperty("by") String by,
                              @JsonProperty("value") Long withValue,
                              @JsonProperty("at") ZonedDateTime at) {

        this.aggregateId = Objects.requireNonNull(aggregateId);
        this.by = Optional.ofNullable(by).orElse("anonymous");
        this.withValue = Optional.ofNullable(withValue).orElse(1L);
        this.at = Optional.ofNullable(at).orElse(ZonedDateTime.now());
    }

    public CounterIncremented(UUID aggregateId) {
        this(aggregateId, null, null, null);
    }
}
