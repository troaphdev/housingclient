package com.housingclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.housingclient.HousingClient;
import com.housingclient.module.Module;

import java.io.*;
import java.util.*;

public class ProfileManager {
    
    private final File profilesDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private String activeProfile = "Default";
    
    // Preset profile names
    private static final String[] DEFAULT_PROFILES = {
        "Default", "Visitor", "Build", "PvP", "Testing"
    };
    
    public ProfileManager(File dataDir) {
        this.profilesDir = new File(dataDir, "profiles");
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
        
        // Create default profiles if they don't exist
        for (String name : DEFAULT_PROFILES) {
            File profileFile = new File(profilesDir, name.toLowerCase() + ".json");
            if (!profileFile.exists()) {
                profiles.put(name, new Profile(name));
            }
        }
    }
    
    public void loadProfiles() {
        profiles.clear();
        
        File[] files = profilesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        Profile profile = Profile.fromJson(json);
                        profiles.put(profile.getName(), profile);
                    }
                } catch (IOException e) {
                    HousingClient.LOGGER.error("Failed to load profile: " + file.getName(), e);
                }
            }
        }
        
        // Ensure we have default profiles
        for (String name : DEFAULT_PROFILES) {
            if (!profiles.containsKey(name)) {
                profiles.put(name, new Profile(name));
            }
        }
        
        // Load active profile from config
        File activeFile = new File(profilesDir, "active.txt");
        if (activeFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(activeFile))) {
                String active = reader.readLine();
                if (active != null && profiles.containsKey(active)) {
                    activeProfile = active;
                }
            } catch (IOException e) {
                HousingClient.LOGGER.error("Failed to load active profile", e);
            }
        }
    }
    
    public void saveProfiles() {
        for (Profile profile : profiles.values()) {
            saveProfile(profile);
        }
        
        // Save active profile
        File activeFile = new File(profilesDir, "active.txt");
        try (FileWriter writer = new FileWriter(activeFile)) {
            writer.write(activeProfile);
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to save active profile", e);
        }
    }
    
    public void saveProfile(Profile profile) {
        File profileFile = new File(profilesDir, profile.getName().toLowerCase() + ".json");
        try (FileWriter writer = new FileWriter(profileFile)) {
            gson.toJson(profile.toJson(), writer);
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to save profile: " + profile.getName(), e);
        }
    }
    
    public Profile getActiveProfile() {
        return profiles.get(activeProfile);
    }
    
    public void setActiveProfile(String name) {
        if (profiles.containsKey(name)) {
            activeProfile = name;
            applyProfile(profiles.get(name));
        }
    }
    
    public void applyProfile(Profile profile) {
        // Apply module states from profile
        for (Module module : HousingClient.getInstance().getModuleManager().getModules()) {
            Boolean state = profile.getModuleState(module.getName());
            if (state != null && state != module.isEnabled()) {
                module.setEnabled(state);
            }
        }
    }
    
    public void saveCurrentToProfile() {
        Profile profile = getActiveProfile();
        if (profile != null) {
            for (Module module : HousingClient.getInstance().getModuleManager().getModules()) {
                profile.setModuleState(module.getName(), module.isEnabled());
            }
            saveProfile(profile);
        }
    }
    
    public Collection<Profile> getProfiles() {
        return profiles.values();
    }
    
    public Profile getProfile(String name) {
        return profiles.get(name);
    }
    
    public void createProfile(String name) {
        if (!profiles.containsKey(name)) {
            Profile profile = new Profile(name);
            profiles.put(name, profile);
            saveProfile(profile);
        }
    }
    
    public void deleteProfile(String name) {
        if (profiles.containsKey(name) && !Arrays.asList(DEFAULT_PROFILES).contains(name)) {
            profiles.remove(name);
            File profileFile = new File(profilesDir, name.toLowerCase() + ".json");
            if (profileFile.exists()) {
                profileFile.delete();
            }
            
            if (activeProfile.equals(name)) {
                activeProfile = "Default";
            }
        }
    }
    
    public String getActiveProfileName() {
        return activeProfile;
    }
}
