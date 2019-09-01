package daggerok.infrastructure;

import lombok.extern.log4j.Log4j2;

import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Optional;

@Log4j2
@Provider
@RequestScoped
public class ErrorMapper implements ExceptionMapper<Exception> {

    @Context
    UriInfo uriInfo;

    @Context
    Request request;

    @Context
    HttpHeaders httpHeaders;

    @Override
    public Response toResponse(Exception exception) {
        log.warn("{} {}", request.getMethod(), exception.getLocalizedMessage(), exception);
        // log.info("{}", httpHeaders.getRequestHeaders());

        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(Json.createObjectBuilder()
                                   .add("error", Optional.ofNullable(exception.getLocalizedMessage())
                                                         .orElse("empty"))
                                   // .add("header", httpHeaders.getRequestHeaders()
                                   //                           .entrySet()
                                   //                           .stream()
                                   //                           .map(entry -> singletonMap(entry.getKey(),
                                   //                                                      entry.getValue()
                                   //                                                           .stream()
                                   //                                                           .collect(Collectors.joining(
                                   //                                                                   ","))))
                                   //                           .map(Map::entrySet)
                                   //                           .filter(entries -> !entries.isEmpty())
                                   //                           .map(entries -> entries.iterator().next())
                                   //                           .map(entry -> Json.createObjectBuilder()
                                   //                                             .add(entry.getKey(), entry.getValue())
                                   //                                             .build())
                                   //                           .collect(JsonCollectors.toJsonArray()))
                                   .add("_links", Json.createObjectBuilder()
                                                      .add("_self", uriInfo.getAbsolutePath()
                                                                           .toString())
                                                      .build())
                                   .build())
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }

    // private void on(@Observes ContainerInitialized containerInitializedEvent) {
    //     log.info(containerInitializedEvent);
    // }
}
