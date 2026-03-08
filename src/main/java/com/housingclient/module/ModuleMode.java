package com.housingclient.module;

public enum ModuleMode {
    
    PLAYER("Player Mode"),
    OWNER("Owner Mode"),
    BOTH("Both Modes");
    
    private final String displayName;
    
    ModuleMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

