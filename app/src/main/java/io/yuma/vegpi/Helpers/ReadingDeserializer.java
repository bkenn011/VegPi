package io.yuma.vegpi.Helpers;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import io.yuma.vegpi.guts.Reading;

/**
 * Created by bkennedy on 1/27/18.
 */

public class ReadingDeserializer implements JsonDeserializer<Reading> {
    @Override
    public Reading deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return new Gson().fromJson(json.getAsJsonObject().get("result").getAsString(),Reading.class);
        } catch  (Exception e) {
            return null;
        }

    }
}
