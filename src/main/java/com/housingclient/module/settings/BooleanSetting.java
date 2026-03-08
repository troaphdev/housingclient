package com.housingclient.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {

    // Animation state for GUI (0.0 to 1.0)
    public float animation = 0f;

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
        this.animation = defaultValue ? 1.0f : 0.0f;
    }

    public boolean isEnabled() {
        return getValue();
    }

    public void toggle() {
        setValue(!getValue());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) {
            setValue(element.getAsBoolean());
        }
    }
}
