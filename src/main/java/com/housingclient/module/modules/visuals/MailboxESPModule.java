package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.utils.RenderUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;

import java.awt.Color;

/**
 * Purely visual locator for Hypixel Housing mailbox furniture/holograms.
 */
public class MailboxESPModule extends Module {

    private static final String MAILBOX_NAME = "Mailbox";
    private static final Color MAILBOX_ORANGE = new Color(255, 170, 0); // Minecraft &6
    private static final float TRACER_WIDTH = 1.5F;
    private static final float BOX_LINE_WIDTH = 2.0F;

    public MailboxESPModule() {
        super("Mailbox ESP", "Draw an orange tracer and ESP box around Housing mailboxes",
                Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!isMailboxHologram(entity)) {
                continue;
            }

            RenderUtils.drawTracer(
                    entity,
                    partialTicks,
                    MAILBOX_ORANGE.getRGB(),
                    TRACER_WIDTH,
                    RenderUtils.getCameraPos(-0.2));
            drawMailboxBox(entity, partialTicks);
        }
    }

    /**
     * Hypixel's 1.8-compatible holograms/furniture are represented by armor
     * stands. Match the complete visible name after removing formatting so
     * names such as "\u00A7fMailbox" match while "Mailbox Settings" does not.
     */
    private boolean isMailboxHologram(Entity entity) {
        if (!(entity instanceof EntityArmorStand) || entity.getEntityId() < 0) {
            return false;
        }

        return isExactMailboxName(entity.getCustomNameTag())
                || isExactMailboxName(entity.getName())
                || (entity.getDisplayName() != null
                        && isExactMailboxName(entity.getDisplayName().getFormattedText()));
    }

    private boolean isExactMailboxName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        if (stripped == null) {
            return false;
        }

        // Also tolerate ampersand formatting if a server/plugin exposes it
        // literally rather than converting it to the section symbol.
        stripped = stripped.replaceAll("(?i)&[0-9A-FK-OR]", "").trim();
        return MAILBOX_NAME.equalsIgnoreCase(stripped);
    }

    private void drawMailboxBox(Entity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        x -= mc.getRenderManager().viewerPosX;
        y -= mc.getRenderManager().viewerPosY;
        z -= mc.getRenderManager().viewerPosZ;

        AxisAlignedBB entityBox = entity.getEntityBoundingBox();
        AxisAlignedBB box = entityBox
                .offset(-entity.posX, -entity.posY, -entity.posZ)
                .offset(x, y, z);

        // Marker-style hologram stands can report an almost zero-sized box.
        // Give those a normal armor-stand-sized outline so the ESP stays useful.
        if (box.maxX - box.minX < 0.1D || box.maxY - box.minY < 0.1D) {
            box = new AxisAlignedBB(
                    x - 0.35D, y, z - 0.35D,
                    x + 0.35D, y + 1.9D, z + 0.35D);
        }

        RenderUtils.drawOutlinedBox(box, MAILBOX_ORANGE, BOX_LINE_WIDTH);
    }
}
