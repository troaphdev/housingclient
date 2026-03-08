package com.housingclient.gui.alt;

import com.housingclient.altmanager.CookieAltManager;

import com.housingclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Modern Alt Manager GUI - matches HousingClient's ClickGUI dark-panel
 * aesthetic.
 * Full-width account cards with skin head icons, status indicators, color-coded
 * action buttons, keyboard navigation, smooth scrolling, filter tabs, and alt
 * type labels.
 */
public class GuiAltManager extends GuiScreen {

    // == Panel Dimensions ==
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 340;
    private static final int CORNER_RADIUS = 12;
    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 52;

    // == Card Dimensions ==
    private static final int CARD_HEIGHT = 36;
    private static final int CARD_GAP = 4;
    private static final int CARD_MARGIN = 14;
    private static final int CARD_RADIUS = 6;

    // == Button Dimensions ==
    private static final int BTN_WIDTH = 90;
    private static final int BTN_HEIGHT = 26;
    private static final int BTN_RADIUS = 6;
    private static final int BTN_GAP = 10;

    // == Head Avatar ==
    private static final int HEAD_SIZE = 20;
    private static final int HEAD_RADIUS = 4;

    // == Colors (matching ClickGUI) ==
    private static final int BG_COLOR = 0xF2141414;
    private static final int BORDER_COLOR = 0xFF333333;
    private static final int HEADER_BG = 0xFF1A1A1A;
    private static final int CARD_BG = 0xFF1E1E1E;
    private static final int CARD_HOVER = 0xFF282828;
    private static final int CARD_SELECTED = 0xFF1B2E1B;
    private static final int CARD_SELECTED_BORDER = 0xFF2ECC71;
    private static final int ACTIVE_BADGE_BG = 0xFF1B3A4B;
    private static final int ACTIVE_BADGE_TEXT = 0xFF3498DB;

    private static final int ACCENT_GREEN = 0xFF2ECC71;
    private static final int ACCENT_RED = 0xFFE74C3C;
    private static final int ACCENT_BLUE = 0xFF3498DB;
    private static final int TEXT_WHITE = 0xFFF0F0F0;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DIM = 0xFF666666;

    private static final int BTN_LOGIN = 0xFF27AE60;
    private static final int BTN_LOGIN_HOVER = 0xFF2ECC71;
    private static final int BTN_ADD = 0xFF2980B9;
    private static final int BTN_ADD_HOVER = 0xFF3498DB;
    private static final int BTN_DELETE = 0xFFC0392B;
    private static final int BTN_DELETE_HOVER = 0xFFE74C3C;
    private static final int BTN_BACK = 0xFF404040;
    private static final int BTN_BACK_HOVER = 0xFF555555;
    private static final int BTN_DISABLED = 0xFF252525;
    private static final int BTN_DISABLED_TEXT = 0xFF555555;

    private static final int SEARCH_BG = 0xFF252525;
    private static final int SEARCH_BORDER = 0xFF3A3A3A;

    private static final int VALID_DOT = 0xFF2ECC71;
    private static final int INVALID_DOT = 0xFFE74C3C;

    private static final int TAB_ACTIVE_BG = 0xFF2A2A2A;
    private static final int TAB_INACTIVE_BG = 0xFF1A1A1A;

    // == Skin Head Cache ==
    private static final Map<String, ResourceLocation> headCache = new ConcurrentHashMap<>();
    private static final Set<String> headLoading = new HashSet<>();
    private static final ResourceLocation FALLBACK_HEAD = new ResourceLocation("housingclient",
            "textures/gui/default_head.png");

    // == State ==
    private final GuiScreen parent;
    private GuiTextField searchField;
    private List<CookieAltManager.AltAccount> filteredAccounts;
    private CookieAltManager.AltAccount selectedAccount;

    private float scrollOffset = 0;
    private float targetScroll = 0;
    private float maxScroll = 0;
    private long lastClickTime = 0;

    // Panel bounds (computed in initGui)
    private int panelX, panelY;

    // Status animation
    private int statusDots = 0;
    private long lastDotTime = 0;

    // Delete confirmation
    private boolean confirmDelete = false;
    private long confirmDeleteTime = 0;

    public GuiAltManager(GuiScreen parent) {
        this.parent = parent;
        this.filteredAccounts = new ArrayList<>(CookieAltManager.getInstance().getAccounts());
    }

    @Override
    public void initGui() {
        this.filteredAccounts = new ArrayList<>(CookieAltManager.getInstance().getAccounts());
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;

        // Search field inside header
        int searchW = 160;
        int searchX = panelX + PANEL_WIDTH - searchW - 14;
        int searchBoxH = 18;
        int searchY = panelY + (HEADER_HEIGHT - searchBoxH) / 2;
        int textFieldY = searchY + (searchBoxH - 12) / 2;
        searchField = new GuiTextField(0, fontRendererObj, searchX + 5, textFieldY, searchW - 10, 12);
        searchField.setMaxStringLength(32);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setFocused(false);

        // Pre-load heads
        for (CookieAltManager.AltAccount account : filteredAccounts) {
            loadHeadTexture(account);
        }

        updateSearch();
    }

    private void updateButtons() {
        // No vanilla buttons - we draw custom ones
    }

    // ============================================
    // SKIN HEAD LOADING
    // ============================================

    private void loadHeadTexture(CookieAltManager.AltAccount account) {
        String uuid = account.uuid;

        if (uuid == null || uuid.isEmpty()) {
            resolveUuid(account);
            return;
        }

        if (headCache.containsKey(uuid) || headLoading.contains(uuid))
            return;

        synchronized (headLoading) {
            headLoading.add(uuid);
        }

        new Thread(() -> {
            try {
                String cleanUuid = uuid.replace("-", "");
                BufferedImage image = null;

                image = fetchImage("https://mc-heads.net/avatar/" + cleanUuid + "/64");
                if (image == null)
                    image = fetchImage("https://crafatar.com/avatars/" + cleanUuid + "?size=64&overlay");
                if (image == null)
                    image = fetchImage("https://minotar.net/avatar/" + cleanUuid + "/64.png");

                if (image != null) {
                    final BufferedImage rounded = makeRounded(image, 12);
                    mc.addScheduledTask(() -> {
                        DynamicTexture tex = new DynamicTexture(rounded);
                        ResourceLocation loc = mc.getTextureManager()
                                .getDynamicTextureLocation("alt_head_" + cleanUuid, tex);
                        headCache.put(uuid, loc);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                synchronized (headLoading) {
                    headLoading.remove(uuid);
                }
            }
        }).start();
    }

    private BufferedImage makeRounded(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(java.awt.Color.WHITE);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
        g2.setComposite(AlphaComposite.SrcIn);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return output;
    }

    private void resolveUuid(CookieAltManager.AltAccount account) {
        if (account.username == null || headLoading.contains("resolve_" + account.username))
            return;
        synchronized (headLoading) {
            headLoading.add("resolve_" + account.username);
        }
        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + account.username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null)
                        sb.append(line);
                    br.close();
                    com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(sb.toString())
                            .getAsJsonObject();
                    if (json.has("id")) {
                        account.uuid = json.get("id").getAsString();
                        loadHeadTexture(account);
                    }
                }
            } catch (Exception e) {
                // Ignore
            } finally {
                synchronized (headLoading) {
                    headLoading.remove("resolve_" + account.username);
                }
            }
        }).start();
    }

    private BufferedImage fetchImage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() / 100 == 2) {
                return ImageIO.read(conn.getInputStream());
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    // ============================================
    // RENDERING
    // ============================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Smooth scroll
        scrollOffset += (targetScroll - scrollOffset) * 0.3f;
        if (Math.abs(scrollOffset - targetScroll) < 0.5f)
            scrollOffset = targetScroll;

        // == PANEL ==
        RenderUtils.drawRoundedRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, CORNER_RADIUS, BG_COLOR);
        RenderUtils.drawRoundedRectOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, CORNER_RADIUS, BORDER_COLOR,
                1.5f);

        drawHeader(mouseX, mouseY);
        // drawFilterBar(mouseX, mouseY); // Removed
        drawAccountList(mouseX, mouseY);
        drawFooter(mouseX, mouseY);
    }

    private void drawHeader(int mouseX, int mouseY) {
        int hx = panelX;
        int hy = panelY;

        // Header background (top rounded corners)
        RenderUtils.drawRoundedRect(hx, hy, PANEL_WIDTH, HEADER_HEIGHT, CORNER_RADIUS, HEADER_BG,
                true, true, false, false);
        RenderUtils.drawRect(hx, hy + HEADER_HEIGHT - 1, PANEL_WIDTH, 1, BORDER_COLOR);

        // Title
        String title = "\u00A7lACCOUNT MANAGER";
        int titleTextH = 8;
        int titleY = hy + (HEADER_HEIGHT - titleTextH) / 2;
        fontRendererObj.drawStringWithShadow(title, hx + 16, titleY, TEXT_WHITE);

        // Account count badge
        int count = filteredAccounts.size();
        String countText = String.valueOf(count);
        int titleWidth = fontRendererObj.getStringWidth(title);
        int badgeH = 14;
        int badgeW = fontRendererObj.getStringWidth(countText) + 10;
        int badgeX = hx + 16 + titleWidth + 8;
        int badgeY = titleY + (titleTextH - badgeH) / 2;
        RenderUtils.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 4, ACCENT_BLUE);
        fontRendererObj.drawStringWithShadow(countText,
                badgeX + (badgeW - fontRendererObj.getStringWidth(countText)) / 2,
                badgeY + (badgeH - titleTextH) / 2, TEXT_WHITE);

        // Search field background
        int searchW = 160;
        int searchX = panelX + PANEL_WIDTH - searchW - 14;
        int searchBoxH = 18;
        int searchY = panelY + (HEADER_HEIGHT - searchBoxH) / 2;
        RenderUtils.drawRoundedRect(searchX, searchY, searchW, searchBoxH, 4, SEARCH_BG);
        RenderUtils.drawRoundedRectOutline(searchX, searchY, searchW, searchBoxH, 4,
                searchField.isFocused() ? ACCENT_BLUE : SEARCH_BORDER, 1.0f);

        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            int phY = searchY + (searchBoxH - titleTextH) / 2;
            fontRendererObj.drawStringWithShadow("\u00A77Search...", searchX + 5, phY, TEXT_DIM);
        }
        searchField.drawTextBox();
    }

    private void drawAccountList(int mouseX, int mouseY) {
        int listX = panelX + CARD_MARGIN;
        int listY = panelY + HEADER_HEIGHT + 4;
        int listW = PANEL_WIDTH - CARD_MARGIN * 2;
        int listH = PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 10;

        // Scissor for scrolling (pad 2px for hover outlines)
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(listX - 2, listY, listW + 4, listH);

        float currentY = listY - scrollOffset;
        String currentUser = mc.getSession().getUsername();

        if (filteredAccounts.isEmpty()) {
            String emptyText = searchField.getText().isEmpty() ? "No accounts added yet" : "No matches found";
            int textW = fontRendererObj.getStringWidth(emptyText);
            fontRendererObj.drawStringWithShadow(emptyText,
                    panelX + (PANEL_WIDTH - textW) / 2, listY + listH / 2 - 4, TEXT_DIM);
        }

        for (int i = 0; i < filteredAccounts.size(); i++) {
            CookieAltManager.AltAccount account = filteredAccounts.get(i);
            float cardY = currentY + i * (CARD_HEIGHT + CARD_GAP);

            if (cardY + CARD_HEIGHT < listY || cardY > listY + listH)
                continue;

            boolean hovered = mouseX >= listX && mouseX <= listX + listW
                    && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT
                    && mouseY >= listY && mouseY <= listY + listH;
            boolean selected = selectedAccount == account;
            boolean isActive = currentUser.equals(account.username);

            // Card background
            int cardBg = selected ? CARD_SELECTED : (hovered ? CARD_HOVER : CARD_BG);
            RenderUtils.drawRoundedRect(listX, cardY, listW, CARD_HEIGHT, CARD_RADIUS, cardBg);

            // Selected accent border (left stripe)
            if (selected) {
                RenderUtils.drawRoundedRect(listX + 4, cardY + 6, 3, CARD_HEIGHT - 12, 2, CARD_SELECTED_BORDER);
            }

            // Hover outline
            if (hovered && !selected) {
                RenderUtils.drawRoundedRectOutline(listX, cardY, listW, CARD_HEIGHT, CARD_RADIUS, 0x40FFFFFF, 1.0f);
            }

            // == Player head ==
            int headX = listX + 12;
            int headY = (int) cardY + (CARD_HEIGHT - HEAD_SIZE) / 2;

            ResourceLocation headTex = headCache.get(account.uuid);
            if (headTex != null) {
                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
                mc.getTextureManager().bindTexture(headTex);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                drawModalRectWithCustomSizedTexture(headX, headY, 0, 0, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);
            } else {
                int charHash = account.username.hashCode();
                int avatarR = (Math.abs(charHash) % 128) + 80;
                int avatarG = (Math.abs(charHash >> 8) % 128) + 80;
                int avatarB = (Math.abs(charHash >> 16) % 128) + 80;
                int avatarColor = 0xFF000000 | (avatarR << 16) | (avatarG << 8) | avatarB;
                RenderUtils.drawRoundedRect(headX, headY, HEAD_SIZE, HEAD_SIZE, HEAD_RADIUS, avatarColor);
                String initial = account.username.substring(0, 1).toUpperCase();
                int initialW = fontRendererObj.getStringWidth(initial);
                fontRendererObj.drawStringWithShadow(initial,
                        headX + (HEAD_SIZE - initialW) / 2, headY + (HEAD_SIZE - 8) / 2, TEXT_WHITE);
                loadHeadTexture(account);
            }

            // == Username ==
            int textX = headX + HEAD_SIZE + 10;
            int textY = (int) cardY + (CARD_HEIGHT - 8) / 2;
            int nameColor = isActive ? ACCENT_BLUE : (account.valid ? TEXT_WHITE : INVALID_DOT);
            fontRendererObj.drawStringWithShadow(account.username, textX, textY, nameColor);

            // == Status Indicators (On Avatar) ==
            if (isActive) {
                // Green dot on bottom-right of avatar
                int dotS = 8;
                int dotX = headX + HEAD_SIZE - dotS + 2;
                int dotY = headY + HEAD_SIZE - dotS + 2;

                // Draw green circle with black outline
                RenderUtils.drawCircle(dotX + dotS / 2.0, dotY + dotS / 2.0, dotS / 2.0 + 1, 0xFF000000); // Outline
                RenderUtils.drawCircle(dotX + dotS / 2.0, dotY + dotS / 2.0, dotS / 2.0, ACCENT_GREEN); // Dot
            } else if (!account.valid) {
                // Red dot on bottom-right of avatar
                int dotS = 8;
                int dotX = headX + HEAD_SIZE - dotS + 2;
                int dotY = headY + HEAD_SIZE - dotS + 2;

                // Draw red circle with black outline
                RenderUtils.drawCircle(dotX + dotS / 2.0, dotY + dotS / 2.0, dotS / 2.0 + 1, 0xFF000000); // Outline
                RenderUtils.drawCircle(dotX + dotS / 2.0, dotY + dotS / 2.0, dotS / 2.0, ACCENT_RED); // Dot
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Update max scroll
        int totalContentH = filteredAccounts.size() * (CARD_HEIGHT + CARD_GAP);
        maxScroll = Math.max(0, totalContentH - listH);
        targetScroll = MathHelper.clamp_float(targetScroll, 0, maxScroll);

        // Scrollbar
        if (maxScroll > 0 && totalContentH > 0) {
            int scrollBarX = panelX + PANEL_WIDTH - 6;
            int scrollBarH = listH;
            float thumbRatio = (float) listH / totalContentH;
            int thumbH = Math.max(20, (int) (scrollBarH * thumbRatio));
            int thumbY = listY + (int) ((scrollBarH - thumbH) * (scrollOffset / maxScroll));

            RenderUtils.drawRoundedRect(scrollBarX, listY, 3, scrollBarH, 2, 0x20FFFFFF);
            RenderUtils.drawRoundedRect(scrollBarX, thumbY, 3, thumbH, 2, 0x60FFFFFF);
        }
    }

    private void drawStatusBar(int rightX, int y) {
        CookieAltManager.Status status = CookieAltManager.getInstance().getStatus();
        if (status == CookieAltManager.Status.IDLE || status == CookieAltManager.Status.SUCCESS)
            return;

        long now = System.currentTimeMillis();
        if (now - lastDotTime > 400) {
            statusDots = (statusDots + 1) % 4;
            lastDotTime = now;
        }

        String statusText;
        if (status == CookieAltManager.Status.FAILED) {
            String err = CookieAltManager.getInstance().getLastError();
            if (err != null && !err.isEmpty()) {
                // Truncate if too long to fit in header
                if (err.length() > 50)
                    err = err.substring(0, 47) + "...";
                statusText = err;
            } else {
                statusText = status.getMessage();
            }
        } else {
            String dots = "...".substring(0, statusDots);
            statusText = status.getMessage() + dots;
        }

        int textW = fontRendererObj.getStringWidth(statusText);

        // Draw aligned to rightX
        fontRendererObj.drawStringWithShadow(statusText, rightX - textW, y, TEXT_GRAY);
    }

    // Removed old drawStatusBar() since we pass coordinates now
    private void drawStatusBar_OLD() {

        // Status drawing completely handled in drawStatusBar(x,y)
    }

    private void drawFooter(int mouseX, int mouseY) {
        int fy = panelY + PANEL_HEIGHT - FOOTER_HEIGHT;
        RenderUtils.drawRect(panelX + CARD_MARGIN, fy, PANEL_WIDTH - CARD_MARGIN * 2, 1, BORDER_COLOR);

        int totalBtnW = BTN_WIDTH * 4 + BTN_GAP * 3;
        int btnX = panelX + (PANEL_WIDTH - totalBtnW) / 2;
        int btnY = fy + (FOOTER_HEIGHT - BTN_HEIGHT) / 2 + 1;

        boolean hasSelection = selectedAccount != null;

        // Login
        drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT,
                "Login", hasSelection ? BTN_LOGIN : BTN_DISABLED,
                hasSelection ? BTN_LOGIN_HOVER : BTN_DISABLED,
                hasSelection ? TEXT_WHITE : BTN_DISABLED_TEXT,
                mouseX, mouseY, hasSelection);

        // Add Account
        btnX += BTN_WIDTH + BTN_GAP;
        drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT,
                "Add Account", BTN_ADD, BTN_ADD_HOVER, TEXT_WHITE,
                mouseX, mouseY, true);

        // Delete
        btnX += BTN_WIDTH + BTN_GAP;
        String deleteLabel = confirmDelete ? "Confirm?" : "Delete";
        int delBg = confirmDelete ? ACCENT_RED : (hasSelection ? BTN_DELETE : BTN_DISABLED);
        int delHover = confirmDelete ? 0xFFFF5544 : (hasSelection ? BTN_DELETE_HOVER : BTN_DISABLED);
        drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT,
                deleteLabel, delBg, delHover,
                hasSelection ? TEXT_WHITE : BTN_DISABLED_TEXT,
                mouseX, mouseY, hasSelection);

        // Back
        btnX += BTN_WIDTH + BTN_GAP;
        drawPillButton(btnX, btnY, BTN_WIDTH, BTN_HEIGHT,
                "Back", BTN_BACK, BTN_BACK_HOVER, TEXT_WHITE,
                mouseX, mouseY, true);

        if (confirmDelete && System.currentTimeMillis() - confirmDeleteTime > 3000) {
            confirmDelete = false;
        }
    }

    private void drawPillButton(int x, int y, int w, int h,
            String label, int bgColor, int hoverColor, int textColor,
            int mouseX, int mouseY, boolean enabled) {
        boolean hovered = enabled && mouseX >= x && mouseX <= x + w && mouseY >= y
                && mouseY <= y + h;

        int bg = hovered ? hoverColor : bgColor;
        RenderUtils.drawRoundedRect(x, y, w, h, BTN_RADIUS, bg);
        if (hovered) {
            RenderUtils.drawRoundedRectOutline(x, y, w, h, BTN_RADIUS, 0x30FFFFFF, 1.0f);
        }
        int textW = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label,
                x + (w - textW) / 2, y + (h - 8) / 2, textColor);
    }

    // == Add Account Popup ==

    // ============================================
    // INPUT HANDLING
    // ============================================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {

        if (mouseButton != 0)
            return;

        // == Account card clicks ==
        int listX = panelX + CARD_MARGIN;
        int listY = panelY + HEADER_HEIGHT + 4;
        int listW = PANEL_WIDTH - CARD_MARGIN * 2;
        int listH = PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 10;

        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            float currentY = listY - scrollOffset;
            for (int i = 0; i < filteredAccounts.size(); i++) {
                float cardY = currentY + i * (CARD_HEIGHT + CARD_GAP);
                if (mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT && cardY + CARD_HEIGHT > listY
                        && cardY < listY + listH) {
                    CookieAltManager.AltAccount clicked = filteredAccounts.get(i);

                    long now = System.currentTimeMillis();
                    if (selectedAccount == clicked && now - lastClickTime < 400) {
                        loginSelected();
                    }
                    lastClickTime = now;
                    selectedAccount = clicked;
                    confirmDelete = false;
                    return;
                }
            }
        }

        // == Footer button clicks ==
        int fy = panelY + PANEL_HEIGHT - FOOTER_HEIGHT;
        int totalBtnW = BTN_WIDTH * 4 + BTN_GAP * 3;
        int btnX = panelX + (PANEL_WIDTH - totalBtnW) / 2;
        int btnY = fy + (FOOTER_HEIGHT - BTN_HEIGHT) / 2 + 1;

        boolean hasSelection = selectedAccount != null;

        // Login
        if (hasSelection && isInButton(mouseX, mouseY, btnX, btnY)) {
            loginSelected();
            return;
        }

        // Add Account -> Direct to Cookie Login
        btnX += BTN_WIDTH + BTN_GAP;
        if (isInButton(mouseX, mouseY, btnX, btnY)) {
            mc.displayGuiScreen(new GuiCookieLogin(this));
            return;
        }

        // Delete
        btnX += BTN_WIDTH + BTN_GAP;
        if (hasSelection && isInButton(mouseX, mouseY, btnX, btnY)) {
            if (confirmDelete) {
                CookieAltManager.getInstance().removeAccount(selectedAccount);
                selectedAccount = null;
                confirmDelete = false;
                updateSearch();
            } else {
                confirmDelete = true;
                confirmDeleteTime = System.currentTimeMillis();
            }
            return;
        }

        // Back
        btnX += BTN_WIDTH + BTN_GAP;
        if (isInButton(mouseX, mouseY, btnX, btnY)) {
            mc.displayGuiScreen(parent);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isInButton(int mx, int my, int bx, int by) {
        return mx >= bx && mx <= bx + BTN_WIDTH && my >= by && my <= by + BTN_HEIGHT;
    }

    private void loginSelected() {
        if (selectedAccount == null)
            return;
        final CookieAltManager.AltAccount account = selectedAccount;
        new Thread(() -> CookieAltManager.getInstance().loginWithAccount(account)).start();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            dWheel = Integer.compare(dWheel, 0);
            targetScroll -= dWheel * 30;
            targetScroll = MathHelper.clamp_float(targetScroll, 0, maxScroll);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Close popups with Escape
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }

        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            updateSearch();
            return;
        }

        switch (keyCode) {
            case Keyboard.KEY_UP:
                selectPrevious();
                return;
            case Keyboard.KEY_DOWN:
                selectNext();
                return;
            case Keyboard.KEY_RETURN:
                loginSelected();
                return;
            case Keyboard.KEY_DELETE:
                if (selectedAccount != null) {
                    if (confirmDelete) {
                        CookieAltManager.getInstance().removeAccount(selectedAccount);
                        selectedAccount = null;
                        confirmDelete = false;
                        updateSearch();
                    } else {
                        confirmDelete = true;
                        confirmDeleteTime = System.currentTimeMillis();
                    }
                }
                return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void selectPrevious() {
        if (filteredAccounts.isEmpty())
            return;
        if (selectedAccount == null) {
            selectedAccount = filteredAccounts.get(filteredAccounts.size() - 1);
        } else {
            int idx = filteredAccounts.indexOf(selectedAccount);
            if (idx > 0)
                selectedAccount = filteredAccounts.get(idx - 1);
        }
        confirmDelete = false;
        ensureVisible();
    }

    private void selectNext() {
        if (filteredAccounts.isEmpty())
            return;
        if (selectedAccount == null) {
            selectedAccount = filteredAccounts.get(0);
        } else {
            int idx = filteredAccounts.indexOf(selectedAccount);
            if (idx < filteredAccounts.size() - 1)
                selectedAccount = filteredAccounts.get(idx + 1);
        }
        confirmDelete = false;
        ensureVisible();
    }

    private void ensureVisible() {
        if (selectedAccount == null)
            return;
        int idx = filteredAccounts.indexOf(selectedAccount);
        int listH = PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 10;
        float cardTop = idx * (CARD_HEIGHT + CARD_GAP);
        float cardBottom = cardTop + CARD_HEIGHT;

        if (cardTop < targetScroll) {
            targetScroll = cardTop;
        } else if (cardBottom > targetScroll + listH) {
            targetScroll = cardBottom - listH;
        }
        targetScroll = MathHelper.clamp_float(targetScroll, 0, maxScroll);
    }

    private void updateSearch() {
        String search = searchField.getText().toLowerCase();
        List<CookieAltManager.AltAccount> allAccounts = CookieAltManager.getInstance().getAccounts();

        filteredAccounts = allAccounts.stream()
                .filter(a -> a.username.toLowerCase().contains(search))
                // Sort: valid accounts first, invalid at bottom
                .sorted((a, b) -> {
                    if (a.valid == b.valid)
                        return 0;
                    return a.valid ? -1 : 1;
                })
                .collect(Collectors.toList());

        if (selectedAccount != null && !filteredAccounts.contains(selectedAccount)) {
            selectedAccount = null;
        }
        targetScroll = 0;
        scrollOffset = 0;
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
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
