package com.housingclient.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for inserting client-side text without leaking the inserted
 * formatting into the text that follows it.
 */
public final class MinecraftFormatting {

    private static final char SECTION = '\u00A7';

    private MinecraftFormatting() {
    }

    /**
     * Appends a formatted suffix and then restores the formatting that was
     * active at the end of the original text.
     */
    public static String appendPreservingFormatting(String text, String suffix) {
        if (text == null || text.isEmpty() || suffix == null || suffix.isEmpty()) {
            return text;
        }
        return text + suffix + activeFormattingAt(text, text.length());
    }

    /**
     * Appends a formatted insertion after every case-insensitive occurrence,
     * restoring the formatting active immediately after each original match.
     */
    public static String appendAfterIgnoreCase(String text, String target, String insertion) {
        if (text == null || text.isEmpty() || target == null || target.isEmpty()
                || insertion == null || insertion.isEmpty()) {
            return text;
        }

        Matcher matcher = Pattern.compile(Pattern.quote(target), Pattern.CASE_INSENSITIVE).matcher(text);
        StringBuffer result = new StringBuffer();
        boolean changed = false;

        while (matcher.find()) {
            if (text.regionMatches(matcher.end(), insertion, 0, insertion.length())) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            String replacement = matcher.group()
                    + insertion
                    + activeFormattingAt(text, matcher.end());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            changed = true;
        }

        if (!changed) {
            return text;
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Returns codes that recreate the Minecraft color and style state active at
     * {@code endIndex}. A reset is used when no explicit color is active so the
     * renderer returns to its original draw color rather than remaining on an
     * inserted color.
     */
    public static String activeFormattingAt(String text, int endIndex) {
        if (text == null || text.isEmpty()) {
            return String.valueOf(SECTION) + 'r';
        }

        int limit = Math.max(0, Math.min(endIndex, text.length()));
        char color = 0;
        boolean obfuscated = false;
        boolean bold = false;
        boolean strikethrough = false;
        boolean underline = false;
        boolean italic = false;

        for (int i = 0; i + 1 < limit; i++) {
            if (text.charAt(i) != SECTION) {
                continue;
            }

            char code = Character.toLowerCase(text.charAt(++i));
            if (isColor(code)) {
                color = code;
                obfuscated = false;
                bold = false;
                strikethrough = false;
                underline = false;
                italic = false;
                continue;
            }

            switch (code) {
                case 'k':
                    obfuscated = true;
                    break;
                case 'l':
                    bold = true;
                    break;
                case 'm':
                    strikethrough = true;
                    break;
                case 'n':
                    underline = true;
                    break;
                case 'o':
                    italic = true;
                    break;
                case 'r':
                    color = 0;
                    obfuscated = false;
                    bold = false;
                    strikethrough = false;
                    underline = false;
                    italic = false;
                    break;
                default:
                    break;
            }
        }

        StringBuilder restoration = new StringBuilder();
        restoration.append(SECTION).append(color == 0 ? 'r' : color);
        appendStyle(restoration, obfuscated, 'k');
        appendStyle(restoration, bold, 'l');
        appendStyle(restoration, strikethrough, 'm');
        appendStyle(restoration, underline, 'n');
        appendStyle(restoration, italic, 'o');
        return restoration.toString();
    }

    private static boolean isColor(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private static void appendStyle(StringBuilder builder, boolean enabled, char code) {
        if (enabled) {
            builder.append(SECTION).append(code);
        }
    }
}
