package com.housingclient.mixin;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEditSign.class)
public abstract class MixinGuiEditSign extends GuiScreen {

    @Shadow
    private TileEntitySign field_146848_f; // tileSign

    @Shadow
    private int field_146851_h; // editLine

    @Inject(method = "func_73869_a", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        // Ctrl+V or Cmd+V paste support
        if (isCtrlKeyDown() && (keyCode == 47)) { // 47 = V key
            String clipboard = getClipboardString();
            if (clipboard != null && !clipboard.isEmpty()) {
                TileEntitySign sign = this.field_146848_f;
                int line = this.field_146851_h;
                if (sign != null && line >= 0 && line < 4) {
                    String current = sign.signText[line] != null
                            ? sign.signText[line].getUnformattedText()
                            : "";
                    // Remove newlines from clipboard, take first line only
                    String pasteText = clipboard.split("\n")[0].split("\r")[0];
                    String newText = current + pasteText;
                    // Sign line limit is 15 characters in vanilla, but we allow any text
                    if (newText.length() > 90) {
                        newText = newText.substring(0, 90);
                    }
                    sign.signText[line] = new ChatComponentText(newText);
                }
                ci.cancel();
            }
        }
    }
}
