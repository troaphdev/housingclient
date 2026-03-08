package com.housingclient.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Profile {
    
    private final String name;
    private final Map<String, Boolean> moduleStates = new HashMap<>();
    private final Map<String, JsonObject> moduleSettings = new HashMap<>();
    
    public Profile(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public Boolean getModuleState(String moduleName) {
        return moduleStates.get(moduleName);
    }
    
    public void setModuleState(String moduleName, boolean enabled) {
        moduleStates.put(moduleName, enabled);
    }
    
    public JsonObject getModuleSettings(String moduleName) {
        return moduleSettings.get(moduleName);
    }
    
    public void setModuleSettings(String moduleName, JsonObject settings) {
        moduleSettings.put(moduleName, settings);
    }
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        
        JsonObject states = new JsonObject();
        for (Map.Entry<String, Boolean> entry : moduleStates.entrySet()) {
            states.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("moduleStates", states);
        
        JsonObject settings = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : moduleSettings.entrySet()) {
            settings.add(entry.getKey(), entry.getValue());
        }
        json.add("moduleSettings", settings);
        
        return json;
    }
    
    public static Profile fromJson(JsonObject json) {
        String name = json.has("name") ? json.get("name").getAsString() : "Unknown";
        Profile profile = new Profile(name);
        
        if (json.has("moduleStates")) {
            JsonObject states = json.getAsJsonObject("moduleStates");
            for (Map.Entry<String, JsonElement> entry : states.entrySet()) {
                profile.setModuleState(entry.getKey(), entry.getValue().getAsBoolean());
            }
        }
        
        if (json.has("moduleSettings")) {
            JsonObject settings = json.getAsJsonObject("moduleSettings");
            for (Map.Entry<String, JsonElement> entry : settings.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    profile.setModuleSettings(entry.getKey(), entry.getValue().getAsJsonObject());
                }
            }
        }
        
        return profile;
    }
}

