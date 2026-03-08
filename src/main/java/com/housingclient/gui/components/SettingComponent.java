package com.housingclient.gui.components;

import com.housingclient.gui.theme.Theme;
import com.housingclient.module.settings.Setting;

public abstract class SettingComponent {
    
    protected int x, y, width;
    protected final Theme theme;
    
    public SettingComponent(Theme theme) {
        this.theme = theme;
    }
    
    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }
    
    public abstract void draw(int mouseX, int mouseY);
    
    public abstract void mouseClicked(int mouseX, int mouseY, int button);
    
    public abstract void mouseReleased(int mouseX, int mouseY, int button);
    
    public abstract int getHeight();
    
    public abstract Setting<?> getSetting();
}

