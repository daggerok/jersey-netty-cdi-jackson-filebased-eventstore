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

    @Override
    public Response toResponse(Exception exception) {
        log.warn("{} {}", request.getMethod(), exception.getLocalizedMessage(), exception);

        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(Json.createObjectBuilder()
                                   .add("error", Optional.ofNullable(exception.getLocalizedMessage())
                                                         .orElse("empty"))
                                   .add("_links", Json.createObjectBuilder()
                                                      .add("_self", uriInfo.getAbsolutePath()
                                                                           .toString())
                                                      .build())
                                   .build())
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}
