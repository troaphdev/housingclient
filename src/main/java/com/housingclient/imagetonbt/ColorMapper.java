package com.housingclient.imagetonbt;

public class ColorMapper {
    // Minecraft color codes with their RGB values
    private static final int[][] MC_COLORS = {
            { 0, 0, 0 }, // §0 - Black
            { 0, 0, 170 }, // §1 - Dark Blue
            { 0, 170, 0 }, // §2 - Dark Green
            { 0, 170, 170 }, // §3 - Dark Aqua
            { 170, 0, 0 }, // §4 - Dark Red
            { 170, 0, 170 }, // §5 - Dark Purple (magenta)
            { 255, 170, 0 }, // §6 - Gold
            { 170, 170, 170 }, // §7 - Gray
            { 85, 85, 85 }, // §8 - Dark Gray
            { 85, 85, 255 }, // §9 - Blue
            { 85, 255, 85 }, // §a - Green
            { 85, 255, 255 }, // §b - Aqua
            { 255, 85, 85 }, // §c - Red
            { 255, 85, 255 }, // §d - Light Purple (pink/magenta)
            { 255, 255, 85 }, // §e - Yellow
            { 255, 255, 255 } // §f - White
    };

    private static final String[] COLOR_CODES = {
            "\u00A70", "\u00A71", "\u00A72", "\u00A73",
            "\u00A74", "\u00A75", "\u00A76", "\u00A77",
            "\u00A78", "\u00A79", "\u00A7a", "\u00A7b",
            "\u00A7c", "\u00A7d", "\u00A7e", "\u00A7f"
    };

    /**
     * Get the closest Minecraft color code for the given RGB values.
     * Uses CIEDE2000-inspired perceptual color distance for accuracy.
     */
    public static String getColorCode(int red, int green, int blue) {
        int closestIndex = 0;
        double closestDistance = Double.MAX_VALUE;

        // Convert input to LAB for better perceptual matching
        double[] inputLab = rgbToLab(red, green, blue);

        for (int i = 0; i < MC_COLORS.length; i++) {
            double[] colorLab = rgbToLab(MC_COLORS[i][0], MC_COLORS[i][1], MC_COLORS[i][2]);
            double distance = deltaE(inputLab, colorLab);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }

        return COLOR_CODES[closestIndex];
    }

    /**
     * Convert RGB to CIE LAB color space for perceptual color matching
     */
    private static double[] rgbToLab(int r, int g, int b) {
        // RGB to XYZ
        double rn = r / 255.0;
        double gn = g / 255.0;
        double bn = b / 255.0;

        rn = (rn > 0.04045) ? Math.pow((rn + 0.055) / 1.055, 2.4) : rn / 12.92;
        gn = (gn > 0.04045) ? Math.pow((gn + 0.055) / 1.055, 2.4) : gn / 12.92;
        bn = (bn > 0.04045) ? Math.pow((bn + 0.055) / 1.055, 2.4) : bn / 12.92;

        double x = rn * 0.4124564 + gn * 0.3575761 + bn * 0.1804375;
        double y = rn * 0.2126729 + gn * 0.7151522 + bn * 0.0721750;
        double z = rn * 0.0193339 + gn * 0.1191920 + bn * 0.9503041;

        // XYZ to LAB (D65 illuminant)
        x /= 0.95047;
        y /= 1.0;
        z /= 1.08883;

        x = (x > 0.008856) ? Math.pow(x, 1.0 / 3.0) : (7.787 * x) + (16.0 / 116.0);
        y = (y > 0.008856) ? Math.pow(y, 1.0 / 3.0) : (7.787 * y) + (16.0 / 116.0);
        z = (z > 0.008856) ? Math.pow(z, 1.0 / 3.0) : (7.787 * z) + (16.0 / 116.0);

        double L = (116.0 * y) - 16.0;
        double a = 500.0 * (x - y);
        double bVal = 200.0 * (y - z);

        return new double[] { L, a, bVal };
    }

    /**
     * Calculate Delta E (CIE76) color difference - perceptually uniform
     */
    private static double deltaE(double[] lab1, double[] lab2) {
        double dL = lab1[0] - lab2[0];
        double da = lab1[1] - lab2[1];
        double db = lab1[2] - lab2[2];
        return Math.sqrt(dL * dL + da * da + db * db);
    }
}
