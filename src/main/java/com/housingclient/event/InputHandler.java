package com.housingclient.event;

import com.housingclient.HousingClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class InputHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        // Handle shutdown
        if (mc.theWorld == null && HousingClient.getInstance() != null) {
            // Could save on world exit
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        // Track CPS regardless of GUI state (removed thePlayer check)
        // This allows CPS to work in GUIs too
        if (event.button == 0 && event.buttonstate) {
            if (HousingClient.getInstance() != null && HousingClient.getInstance().getHud() != null) {
                HousingClient.getInstance().getHud().registerLeftClick();
            }
        } else if (event.button == 1 && event.buttonstate) {
            if (HousingClient.getInstance() != null && HousingClient.getInstance().getHud() != null) {
                HousingClient.getInstance().getHud().registerRightClick();
            }
        }
    }
}
