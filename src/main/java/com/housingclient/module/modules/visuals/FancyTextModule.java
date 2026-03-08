package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ModeSetting;

import java.util.HashMap;
import java.util.Map;

/**
 * Fancy Text Module
 *
 * Replaces typed characters with fancy Unicode alternatives.
 * Supports three alphabets: Fancy, Circled, and Parenthesized.
 * Includes a setting to revert fancy text back to normal for commands.
 */
public class FancyTextModule extends Module {

    private final ModeSetting alphabet = new ModeSetting("Alphabet", "Select text style",
            "Fancy", "Fancy", "Circled", "Parenthesized");
    private final BooleanSetting revertCommands = new BooleanSetting("Revert Commands",
            "Convert fancy text back to normal when sending / commands", true);

    // Character maps for each alphabet
    private static final Map<Character, Character> FANCY_MAP = new HashMap<>();
    private static final Map<Character, Character> CIRCLED_MAP = new HashMap<>();
    private static final Map<Character, Character> PARENTHESIZED_MAP = new HashMap<>();

    // Reverse maps for command revert
    private static final Map<Character, Character> REVERSE_FANCY = new HashMap<>();
    private static final Map<Character, Character> REVERSE_CIRCLED = new HashMap<>();
    private static final Map<Character, Character> REVERSE_PARENTHESIZED = new HashMap<>();

    static {
        // Fancy alphabet (Mathematical Fraktur / Bold Script mix)
        String fancyUpper = "\uD835\uDC00\uD835\uDC01\uD835\uDC02\uD835\uDC03\uD835\uDC04\uD835\uDC05\uD835\uDC06\uD835\uDC07\uD835\uDC08\uD835\uDC09\uD835\uDC0A\uD835\uDC0B\uD835\uDC0C\uD835\uDC0D\uD835\uDC0E\uD835\uDC0F\uD835\uDC10\uD835\uDC11\uD835\uDC12\uD835\uDC13\uD835\uDC14\uD835\uDC15\uD835\uDC16\uD835\uDC17\uD835\uDC18\uD835\uDC19";
        String fancyLower = "\uD835\uDC1A\uD835\uDC1B\uD835\uDC1C\uD835\uDC1D\uD835\uDC1E\uD835\uDC1F\uD835\uDC20\uD835\uDC21\uD835\uDC22\uD835\uDC23\uD835\uDC24\uD835\uDC25\uD835\uDC26\uD835\uDC27\uD835\uDC28\uD835\uDC29\uD835\uDC2A\uD835\uDC2B\uD835\uDC2C\uD835\uDC2D\uD835\uDC2E\uD835\uDC2F\uD835\uDC30\uD835\uDC31\uD835\uDC32\uD835\uDC33";

        // These are surrogate pairs (astral plane), Minecraft's FontRenderer won't
        // render them.
        // Instead, use fullwidth + special Unicode that ARE in the BMP.
        // Using Mathematical Sans-Serif Bold which maps to BMP-adjacent or
        // use the simpler approach: direct char arrays.

        // Let's use a practical approach: map to characters Minecraft CAN render.
        // Fullwidth Latin characters (FF01-FF5E range) work in Minecraft's font
        // renderer.

        // Fancy: Fullwidth characters (ＡＢＣ... ａｂｃ...)
        char[] fwUpper = new char[26];
        char[] fwLower = new char[26];
        for (int i = 0; i < 26; i++) {
            fwUpper[i] = (char) (0xFF21 + i); // Ａ-Ｚ
            fwLower[i] = (char) (0xFF41 + i); // ａ-ｚ
        }
        for (int i = 0; i < 26; i++) {
            FANCY_MAP.put((char) ('A' + i), fwUpper[i]);
            FANCY_MAP.put((char) ('a' + i), fwLower[i]);
        }
        // Fullwidth digits
        for (int i = 0; i < 10; i++) {
            FANCY_MAP.put((char) ('0' + i), (char) (0xFF10 + i)); // ０-９
        }
        // Fullwidth punctuation
        FANCY_MAP.put('!', '\uFF01'); // ！
        FANCY_MAP.put('?', '\uFF1F'); // ？
        FANCY_MAP.put('.', '\uFF0E'); // ．
        FANCY_MAP.put(',', '\uFF0C'); // ，
        FANCY_MAP.put(':', '\uFF1A'); // ：
        FANCY_MAP.put(';', '\uFF1B'); // ；
        FANCY_MAP.put('/', '\uFF0F'); // ／

        // Circled alphabet: Ⓐ-Ⓩ (U+24B6-U+24CF), ⓐ-ⓩ (U+24D0-U+24E9)
        for (int i = 0; i < 26; i++) {
            CIRCLED_MAP.put((char) ('A' + i), (char) (0x24B6 + i));
            CIRCLED_MAP.put((char) ('a' + i), (char) (0x24D0 + i));
        }
        // Circled digits: ① (U+2460) to ⑨ (U+2468), ⓪ (U+24EA) for 0
        CIRCLED_MAP.put('0', '\u24EA'); // ⓪
        for (int i = 1; i <= 9; i++) {
            CIRCLED_MAP.put((char) ('0' + i), (char) (0x2460 + i - 1));
        }

        // Parenthesized alphabet: ⒜-⒵ (U+249C-U+24B5) — lowercase only
        for (int i = 0; i < 26; i++) {
            PARENTHESIZED_MAP.put((char) ('a' + i), (char) (0x249C + i));
            // Uppercase maps to the same parenthesized lowercase
            PARENTHESIZED_MAP.put((char) ('A' + i), (char) (0x249C + i));
        }
        // Parenthesized digits: ⑴-⑼ (U+2474-U+247C), ⑽ for 10 but we only need 0-9
        // 0 has no standard parenthesized form, fall back to normal
        for (int i = 1; i <= 9; i++) {
            PARENTHESIZED_MAP.put((char) ('0' + i), (char) (0x2474 + i - 1));
        }

        // Build reverse maps
        for (Map.Entry<Character, Character> entry : FANCY_MAP.entrySet()) {
            REVERSE_FANCY.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<Character, Character> entry : CIRCLED_MAP.entrySet()) {
            REVERSE_CIRCLED.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<Character, Character> entry : PARENTHESIZED_MAP.entrySet()) {
            REVERSE_PARENTHESIZED.put(entry.getValue(), entry.getKey());
        }
    }

    public FancyTextModule() {
        super("Fancy Text", "Replaces typed text with fancy Unicode characters", Category.CLIENT, ModuleMode.BOTH);
        addSetting(alphabet);
        addSetting(revertCommands);
    }

    /**
     * Convert a single character to its fancy equivalent using the currently
     * selected alphabet.
     * Returns the original character if no mapping exists.
     */
    public char convertChar(char c) {
        Map<Character, Character> map = getActiveMap();
        Character result = map.get(c);
        return result != null ? result : c;
    }

    /**
     * Convert an entire string to fancy text.
     */
    public String convertString(String input) {
        if (input == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append(convertChar(input.charAt(i)));
        }
        return sb.toString();
    }

    /**
     * Revert a fancy string back to normal ASCII text.
     * Checks all three reverse maps since we don't know which was used.
     */
    public static String revertToNormal(String input) {
        if (input == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            Character normal = REVERSE_FANCY.get(c);
            if (normal == null)
                normal = REVERSE_CIRCLED.get(c);
            if (normal == null)
                normal = REVERSE_PARENTHESIZED.get(c);
            sb.append(normal != null ? normal : c);
        }
        return sb.toString();
    }

    /**
     * Check if a string starts with a command prefix (normal or fancy / and .).
     */
    public static boolean isCommand(String message) {
        if (message == null || message.isEmpty())
            return false;
        char first = message.charAt(0);
        return first == '/' || first == '\uFF0F' || first == '.' || first == '\uFF0E';
    }

    public boolean isRevertCommands() {
        return revertCommands.isEnabled();
    }

    private Map<Character, Character> getActiveMap() {
        String mode = alphabet.getValue();
        switch (mode) {
            case "Circled":
                return CIRCLED_MAP;
            case "Parenthesized":
                return PARENTHESIZED_MAP;
            case "Fancy":
            default:
                return FANCY_MAP;
        }
    }

    public ModeSetting getAlphabetSetting() {
        return alphabet;
    }
}
