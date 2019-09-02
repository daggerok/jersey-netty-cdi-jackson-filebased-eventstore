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
public class CounterSuspended implements DomainEvent {

    private final String eventName = CounterSuspended.class.getSimpleName();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final UUID aggregateId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String by;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String reason;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final ZonedDateTime at;

    @JsonCreator
    public CounterSuspended(@JsonProperty("aggregateId") UUID aggregateId,
                            @JsonProperty("data1") String by,
                            @JsonProperty("data2") String reason,
                            @JsonProperty("at") ZonedDateTime at) {

        this.aggregateId = Objects.requireNonNull(aggregateId);
        this.by = Optional.ofNullable(by).orElse("anonymous");
        this.reason = Optional.ofNullable(reason).orElse("no reason");
        this.at = Optional.ofNullable(at).orElse(ZonedDateTime.now());
    }

    public CounterSuspended(UUID aggregateId) {
        this(aggregateId, null, null, null);
    }
}
