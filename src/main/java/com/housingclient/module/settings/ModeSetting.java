package com.housingclient.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting<String> {
    
    private final String[] modes;
    
    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = modes;
    }
    
    public String[] getModes() {
        return modes;
    }
    
    public int getModeIndex() {
        String current = getValue();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(current)) {
                return i;
            }
        }
        return 0;
    }
    
    public void cycle() {
        int current = getModeIndex();
        int next = (current + 1) % modes.length;
        setValue(modes[next]);
    }
    
    public void cycleReverse() {
        int current = getModeIndex();
        int prev = (current - 1 + modes.length) % modes.length;
        setValue(modes[prev]);
    }
    
    public boolean is(String mode) {
        return getValue().equalsIgnoreCase(mode);
    }
    
    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }
    
    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            for (String mode : modes) {
                if (mode.equals(value)) {
                    setValue(value);
                    return;
                }
            }
        }
    }
}

