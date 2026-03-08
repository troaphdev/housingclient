package com.housingclient.gui.alt;

import com.housingclient.altmanager.CookieAltManager;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;

/**
 * Cookie Login Screen — restyled to match the new Alt Manager dark-panel look.
 */
public class GuiCookieLogin extends GuiScreen {

    // ── Colors (consistent with GuiAltManager) ──
    private static final int BG_COLOR = 0xF2141414;
    private static final int BORDER_COLOR = 0xFF333333;
    private static final int HEADER_BG = 0xFF1A1A1A;
    private static final int TEXT_WHITE = 0xFFF0F0F0;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DIM = 0xFF666666;

    private static final int ACCENT_BLUE = 0xFF3498DB;
    private static final int ACCENT_BLUE_HOVER = 0xFF5DADE2;
    private static final int BTN_BACK = 0xFF404040;
    private static final int BTN_BACK_HOVER = 0xFF555555;
    private static final int BTN_DISABLED = 0xFF252525;
    private static final int ACCENT_GREEN = 0xFF2ECC71;
    private static final int ACCENT_RED = 0xFFE74C3C;

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 220;
    private static final int CORNER_RADIUS = 12;
    private static final int BTN_WIDTH = 140;
    private static final int BTN_HEIGHT = 26;
    private static final int BTN_RADIUS = 6;
    private static final int BTN_GAP = 10;

    private final GuiScreen parent;
    private int panelX, panelY;

    public GuiCookieLogin(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        // Reset status if previous login finished
        CookieAltManager.Status currentStatus = CookieAltManager.getInstance().getStatus();
        if (currentStatus == CookieAltManager.Status.SUCCESS || currentStatus == CookieAltManager.Status.FAILED) {
            CookieAltManager.getInstance().setStatus(CookieAltManager.Status.IDLE);
        }

        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // ── Panel ──
        RenderUtils.drawRoundedRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, CORNER_RADIUS, BG_COLOR);
        RenderUtils.drawRoundedRectOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, CORNER_RADIUS, BORDER_COLOR,
                1.5f);

        // ── Header bar ──
        int headerH = 38;
        RenderUtils.drawRoundedRect(panelX, panelY, PANEL_WIDTH, headerH, CORNER_RADIUS, HEADER_BG, true, true, false,
                false);
        RenderUtils.drawRect(panelX, panelY + headerH - 1, PANEL_WIDTH, 1, BORDER_COLOR);

        String title = "\u00A7lADD ACCOUNT";
        int titleW = fontRendererObj.getStringWidth(title);
        fontRendererObj.drawStringWithShadow(title, panelX + (PANEL_WIDTH - titleW) / 2, panelY + (headerH - 8) / 2,
                TEXT_WHITE);

        // ── Content area ──
        int contentY = panelY + headerH + 16;

        // Current user
        String currentUser = "Logged in as: \u00A7f" + CookieAltManager.getInstance().getCurrentUsername();
        int cuW = fontRendererObj.getStringWidth(currentUser);
        fontRendererObj.drawStringWithShadow(currentUser, panelX + (PANEL_WIDTH - cuW) / 2, contentY, TEXT_GRAY);
        contentY += 18;

        // Status
        CookieAltManager.Status status = CookieAltManager.getInstance().getStatus();
        if (status != CookieAltManager.Status.IDLE) {
            int statusColor;
            switch (status) {
                case SUCCESS:
                    statusColor = ACCENT_GREEN;
                    break;
                case FAILED:
                    statusColor = ACCENT_RED;
                    break;
                default:
                    statusColor = ACCENT_BLUE;
                    break;
            }
            String statusText = status.getMessage();
            int stW = fontRendererObj.getStringWidth(statusText);
            fontRendererObj.drawStringWithShadow(statusText, panelX + (PANEL_WIDTH - stW) / 2, contentY, statusColor);
            contentY += 14;
        }

        // Error message
        if (status == CookieAltManager.Status.FAILED) {
            String err = CookieAltManager.getInstance().getLastError();
            if (err != null && !err.isEmpty()) {
                int maxW = PANEL_WIDTH - 40;
                if (fontRendererObj.getStringWidth(err) > maxW) {
                    err = fontRendererObj.trimStringToWidth(err, maxW - 10) + "...";
                }
                int errW = fontRendererObj.getStringWidth(err);
                fontRendererObj.drawStringWithShadow("\u00A7c" + err, panelX + (PANEL_WIDTH - errW) / 2, contentY,
                        ACCENT_RED);
            }
        }

        // ── Buttons ──
        int totalBtnW = BTN_WIDTH * 2 + BTN_GAP;
        int btnX = panelX + (PANEL_WIDTH - totalBtnW) / 2;
        int btnY = panelY + PANEL_HEIGHT - BTN_HEIGHT - 20;

        // Select & Add button
        drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT,
                "Select Cookie File", ACCENT_BLUE, ACCENT_BLUE_HOVER, TEXT_WHITE,
                mouseX, mouseY);

        // Done button
        drawPillButton(btnX + BTN_WIDTH + BTN_GAP, btnY, BTN_WIDTH, BTN_HEIGHT,
                "Done", BTN_BACK, BTN_BACK_HOVER, TEXT_WHITE,
                mouseX, mouseY);
    }

    private void drawPillButton(int x, int y, int w, int h,
            String label, int bgColor, int hoverColor, int textColor,
            int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = hovered ? hoverColor : bgColor;

        RenderUtils.drawRoundedRect(x, y, w, h, BTN_RADIUS, bg);
        if (hovered) {
            RenderUtils.drawRoundedRectOutline(x, y, w, h, BTN_RADIUS, 0x30FFFFFF, 1.0f);
        }

        int textW = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label,
                x + (w - textW) / 2, y + (h - 8) / 2, textColor);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0)
            return;

        int totalBtnW = BTN_WIDTH * 2 + BTN_GAP;
        int btnX = panelX + (PANEL_WIDTH - totalBtnW) / 2;
        int btnY = panelY + PANEL_HEIGHT - BTN_HEIGHT - 20;

        // Select Cookie File
        if (mouseX >= btnX && mouseX <= btnX + BTN_WIDTH && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT) {
            pickFileAndLogin();
            return;
        }

        // Done
        int doneX = btnX + BTN_WIDTH + BTN_GAP;
        if (mouseX >= doneX && mouseX <= doneX + BTN_WIDTH && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT) {
            mc.displayGuiScreen(parent);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void pickFileAndLogin() {
        new Thread(() -> {
            try {
                // Use PowerShell to open modern Windows file picker
                String psScript = "Add-Type -AssemblyName System.Windows.Forms; " +
                        "$dialog = New-Object System.Windows.Forms.OpenFileDialog; " +
                        "$dialog.Title = 'Select Cookie File'; " +
                        "$dialog.Filter = 'Text Files (*.txt)|*.txt|All Files (*.*)|*.*'; " +
                        "if ($dialog.ShowDialog() -eq 'OK') { Write-Output $dialog.FileName }";

                ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", psScript);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                process.waitFor();

                if (line != null && !line.isEmpty()) {
                    File selectedFile = new File(line.trim());
                    if (selectedFile.exists()) {
                        CookieAltManager.getInstance().addAccountFromCookies(selectedFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                CookieAltManager.getInstance().setStatus(CookieAltManager.Status.FAILED);
            }
        }).start();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
