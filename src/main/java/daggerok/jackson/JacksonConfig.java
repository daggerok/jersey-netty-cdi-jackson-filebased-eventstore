package daggerok.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class JacksonConfig {

    @Produces
    private ObjectMapper objectMapper = JsonMapper.builder()
                                                  .addModules(new JavaTimeModule())
                                                  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                                  .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                                                  .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                                  .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                                                  // .enable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)
                                                  // .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
                                                  .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                                                  .build();
}
