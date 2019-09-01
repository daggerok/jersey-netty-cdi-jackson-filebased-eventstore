package daggerok.infrastructure;

import lombok.extern.log4j.Log4j2;
import org.jboss.weld.environment.se.events.ContainerInitialized;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.WILDCARD;

@Log4j2
@Path("")
@RequestScoped
@Consumes(WILDCARD)
@Produces(APPLICATION_JSON)
public class HealthResource {

    private void on(@Observes ContainerInitialized containerInitializedEvent) {
        log.info(containerInitializedEvent);
    }

    @GET
    @Path("health")
    public JsonObject health() {
        log.info("health");
        return Json.createObjectBuilder()
                   .add("status", "UP")
                   .build();
    }

    @GET
    @Path("{path:(.*)?}")
    // @Path("{path: ^\\.?+$}")
    public JsonObject getAny(@PathParam("path") String path) {
        log.info("unexpected GET");
        return info(path);
    }

    @HEAD
    @Path("{path:(.*)?}")
    public JsonObject headAny(@PathParam("path") String path) {
        log.info("unexpected HEAD");
        return info(path);
    }

    @POST
    @Path("{path:(.*)?}")
    public JsonObject postAny(@PathParam("path") String path) {
        log.info("unexpected POST");
        return info(path);
    }

    @PUT
    @Path("{path:(.*)?}")
    public JsonObject putAny(@PathParam("path") String path) {
        log.info("unexpected PUT");
        return info(path);
    }

    @PATCH
    @Path("{path:(.*)?}")
    public JsonObject patchAny(@PathParam("path") String path) {
        log.info("unexpected PATCH");
        return info(path);
    }

    @DELETE
    @Path("{path:(.*)?}")
    public JsonObject deleteAny(@PathParam("path") String path) {
        log.info("unexpected DELETE");
        return info(path);
    }

    private JsonObject info(String path) {
        return Json.createObjectBuilder()
                   .add("path", "" + path)
                   .build();
    }
}
