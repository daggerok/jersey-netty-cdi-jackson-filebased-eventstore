package daggerok.domain;

import daggerok.eventstore.EventStore;
import daggerok.eventstore.events.CounterCreated;
import daggerok.eventstore.events.CounterIncremented;
import daggerok.eventstore.events.CounterSuspended;
import daggerok.eventstore.events.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@RequestScoped
@Path("counter")
@NoArgsConstructor
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@AllArgsConstructor(onConstructor_ = @Inject)
public class CounterResource {

    @Context
    private UriInfo uriInfo;
    private EventStore eventStore;

    @GET
    public Response findAll() {
        return Response.ok(eventStore.findAll())
                       .build();
    }

    @GET
    @Path("{aggregateId}")
    public Response find(@PathParam("aggregateId") UUID aggregateId) {
        Collection<DomainEvent> history = eventStore.read(aggregateId);
        return Response.ok(Counter.rebuild(new Counter(), history))
                       .build();
    }

    @POST
    public Response createCounter(CounterCreated cmd) {

        Counter counter = new Counter();
        counter.create(cmd.getAggregateId(), cmd.getCounterName());
        eventStore.snapshot(counter);

        URI url = uriInfo.getBaseUriBuilder()
                         .path(CounterResource.class)
                         .path(CounterResource.class, "find")
                         .build(counter.getAggregateId());
        String result = String.format("%s counter created: %s", cmd.getCounterName(), url);

        return Response.created(url)
                       .entity(Json.createObjectBuilder()
                                   .add("result", result)
                                   .build())
                       .build();
    }

    @PUT
    public Response incrementCounter(CounterIncremented cmd) {

        Counter counter = Counter.rebuild(new Counter(), eventStore.read(cmd.getAggregateId()));
        counter.increment(cmd.getBy(), cmd.getWithValue());
        eventStore.snapshot(counter);

        return Response.accepted()
                       .entity(counter)
                       .build();
    }

    @DELETE
    public Response suspendedCounter(CounterSuspended cmd) {

        Counter counter = Counter.rebuild(new Counter(), eventStore.read(cmd.getAggregateId()));
        counter.suspend(cmd.getBy(), cmd.getReason());
        eventStore.snapshot(counter);

        return Response.accepted()
                       .entity(counter)
                       .build();
    }
}
