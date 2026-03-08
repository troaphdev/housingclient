package com.housingclient.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class StringSetting extends Setting<String> {
    
    private final int maxLength;
    
    public StringSetting(String name, String description, String defaultValue) {
        super(name, description, defaultValue);
        this.maxLength = 256;
    }
    
    public StringSetting(String name, String description, String defaultValue, int maxLength) {
        super(name, description, defaultValue);
        this.maxLength = maxLength;
    }
    
    @Override
    public void setValue(String value) {
        if (value.length() > maxLength) {
            value = value.substring(0, maxLength);
        }
        super.setValue(value);
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }
    
    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) {
            setValue(element.getAsString());
        }
    }
}

