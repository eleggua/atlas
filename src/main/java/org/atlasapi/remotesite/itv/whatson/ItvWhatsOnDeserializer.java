package org.atlasapi.remotesite.itv.whatson;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import org.joda.time.DateTime;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.metabroadcast.common.time.DateTimeZones;


public class ItvWhatsOnDeserializer {
    
    private static class ItvDateTimeDeserializer implements JsonDeserializer<DateTime> {
        @Override
        public DateTime deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException {
            String value = json.getAsJsonPrimitive()
                    .getAsString()
                    .replace("/Date(", "")
                    .replace(")/", "");
            return new DateTime(DateTimeZones.UTC).withMillis(Long.parseLong(value));
        }
    }

    private final static Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
        .registerTypeAdapter(DateTime.class, new ItvDateTimeDeserializer())
        .create();
    
    public List<ItvWhatsOnEntry> deserialize(Reader json) {
        return gson.fromJson(json, new TypeToken<List<ItvWhatsOnEntry>>(){}.getType());
    }

}
