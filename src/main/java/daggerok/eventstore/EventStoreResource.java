package daggerok.eventstore;

import daggerok.eventstore.events.CounterCreated;
import daggerok.eventstore.events.CounterIncremented;
import daggerok.eventstore.events.CounterSuspended;
import daggerok.eventstore.events.DomainEvent;
import lombok.extern.log4j.Log4j2;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

@Log4j2
@Path("events")
@ApplicationScoped
public class EventStoreResource {

    private UriInfo uriInfo;
    private EventStore eventStore;

    EventStoreResource() {} // yuk...

    @Inject
    public EventStoreResource(@Context UriInfo uriInfo, EventStore eventStore) {
        this.uriInfo = uriInfo;
        this.eventStore = eventStore;
    }

    private BiFunction<Object, String, Object> require = (variable, variableName) ->
            Optional.ofNullable(variable).orElseThrow(() -> new IllegalArgumentException(
                    String.format("%s is required", variableName)));

    @POST
    public Response createCounter(CounterCreated counterCreated) {
        eventStore.append(counterCreated);

        URI url = uriInfo.getBaseUriBuilder()
                         .path(EventStoreResource.class)
                         .path(EventStoreResource.class, "getCounter")
                         .build(counterCreated.getAggregateId());
        String result = String.format("%s counter created: %s",
                                      counterCreated.getCounterName(), url);
        return Response.created(url)
                       .entity(Json.createObjectBuilder()
                                   .add("result", result)
                                   .add("_links", Json.createArrayBuilder()
                                                      .add(String.format("increment: PUT %s", url))
                                                      .add(String.format("suspend: DELETE %s", url))
                                                      .build())
                                   .build())
                       .build();
    }

    @PUT
    @Path("{aggregateId}")
    public Response incrementCounter(CounterIncremented counterIncremented) {
        require.apply(counterIncremented.getAggregateId(), "aggregateId");
        eventStore.append(counterIncremented);
        return Response.accepted()
                       .entity(Json.createObjectBuilder()
                                   .add("result",
                                        String.format("counter incremented by %s", counterIncremented.getBy()))
                                   .build())
                       .build();
    }

    @DELETE
    @Path("{aggregateId}")
    public Response suspendCounter(CounterSuspended counterSuspended) {
        require.apply(counterSuspended.getAggregateId(), "aggregateId");
        eventStore.append(counterSuspended);
        return Response.accepted()
                       .entity(Json.createObjectBuilder()
                                   .add("result", "counter suspended")
                                   .build())
                       .build();
    }

    @GET
    @Path("{aggregateId}")
    public Response getCounter(@PathParam("aggregateId") UUID aggregateId) {
        log.debug(aggregateId);
        Collection<DomainEvent> events = eventStore.read(aggregateId);
        log.debug(events);
        return Response.ok(events)
                       .build();
    }

    @GET
    @Path("{aggregateId}/collection")
    public Collection<DomainEvent> getCounterCollection(@PathParam("aggregateId") UUID aggregateId) {
        require.apply(aggregateId, "aggregateId is require");
        return eventStore.read(aggregateId);
    }

    @POST
    @Path("collection")
    public Response getCounterCollection(Collection<DomainEvent> events) {
        require.apply(events, "events is require");
        for (DomainEvent domainEvent : events) {
            require.apply(domainEvent.getAggregateId(), "aggregateId is require");
        }
        // eventStore.appendAll(events.toArray(new DomainEvent[0]));
        return Response.accepted()
                       .entity(Json.createObjectBuilder()
                                   .add("result", "all good")
                                   .build())
                       .build();
    }

    @GET
    public Response findAll() {
        return Response.accepted()
                       .entity(eventStore.findAll())
                       .build();
    }

    @DELETE
    public Response cleanup() {
        eventStore.cleanupAll();
        return Response.accepted()
                       .entity(Json.createObjectBuilder()
                                   .add("result", "event store cleared")
                                   .build())
                       .build();
    }
}
