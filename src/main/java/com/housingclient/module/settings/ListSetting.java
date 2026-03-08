package com.housingclient.module.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

public class ListSetting extends Setting<List<String>> {
    
    public ListSetting(String name, String description) {
        super(name, description, new ArrayList<String>());
    }
    
    public ListSetting(String name, String description, List<String> defaultValue) {
        super(name, description, new ArrayList<String>(defaultValue));
    }
    
    public void add(String value) {
        if (!getValue().contains(value)) {
            getValue().add(value);
        }
    }
    
    public void remove(String value) {
        getValue().remove(value);
    }
    
    public boolean contains(String value) {
        return getValue().contains(value);
    }
    
    public void clear() {
        getValue().clear();
    }
    
    public int size() {
        return getValue().size();
    }
    
    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (String s : getValue()) {
            array.add(new JsonPrimitive(s));
        }
        return array;
    }
    
    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonArray()) {
            List<String> list = new ArrayList<String>();
            for (JsonElement e : element.getAsJsonArray()) {
                list.add(e.getAsString());
            }
            setValue(list);
        }
    }
}
