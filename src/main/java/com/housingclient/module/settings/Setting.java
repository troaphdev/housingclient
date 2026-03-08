package com.housingclient.module.settings;

import com.google.gson.JsonElement;

import java.util.function.Supplier;

public abstract class Setting<T> {

    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;
    private Supplier<Boolean> visibility;

    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.visibility = () -> true;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void reset() {
        value = defaultValue;
    }

    public boolean isVisible() {
        return visibility.get();
    }

    public Setting<T> setVisibility(Supplier<Boolean> visibility) {
        this.visibility = visibility;
        return this;
    }

    public void setVisible(boolean visible) {
        this.visibility = () -> visible;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}
