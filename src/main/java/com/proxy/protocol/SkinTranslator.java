package com.proxy.protocol;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Translates skin images between Minecraft and Hytale formats.
 * 
 * Minecraft skins:
 * - Standard format: 64x64 pixels (or 64x32 for legacy)
 * - Uses specific UV mapping for body parts
 * 
 * Hytale skins:
 * - Higher resolution format (typically 128x128 or higher)
 * - May use different UV mapping or layout
 * 
 * This class handles pixel remapping and format conversion between the two.
 */
public class SkinTranslator {

    private static final Logger logger = Logger.getLogger(SkinTranslator.class.getName());

    // Minecraft skin dimensions
    private static final int MC_SKIN_WIDTH = 64;
    private static final int MC_SKIN_HEIGHT = 64;

    // Hytale skin dimensions (adjust based on actual Hytale format)
    private static final int HYTALE_SKIN_WIDTH = 128;
    private static final int HYTALE_SKIN_HEIGHT = 128;

    /**
     * Translates a Minecraft 64x64 skin to Hytale format.
     * 
     * Strategy:
     * 1. Load the Minecraft skin image
     * 2. Scale up to Hytale resolution (2x upscale)
     * 3. Remap pixels according to Hytale's UV layout if needed
     * 4. Return as byte array (PNG format)
     * 
     * @param mcSkin Raw image bytes (PNG format) of a 64x64 Minecraft skin
     * @return Byte array containing the Hytale-compatible skin image
     * @throws IOException If image processing fails
     */
    public byte[] translateMcToHytale(byte[] mcSkin) throws IOException {
        if (mcSkin == null || mcSkin.length == 0) {
            throw new IllegalArgumentException("Minecraft skin data is null or empty");
        }

        // Load Minecraft skin image
        BufferedImage mcImage = ImageIO.read(new ByteArrayInputStream(mcSkin));
        if (mcImage == null) {
            throw new IOException("Failed to decode Minecraft skin image");
        }

        logger.info(String.format("Translating MC skin: %dx%d -> Hytale format",
                mcImage.getWidth(), mcImage.getHeight()));

        // Create Hytale-sized image
        BufferedImage hytaleImage = new BufferedImage(
                HYTALE_SKIN_WIDTH,
                HYTALE_SKIN_HEIGHT,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = hytaleImage.createGraphics();
        try {
            // Enable high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Scale the Minecraft skin to Hytale size
            // Simple 2x upscale (64x64 -> 128x128)
            g2d.drawImage(mcImage, 0, 0, HYTALE_SKIN_WIDTH, HYTALE_SKIN_HEIGHT, null);

            // TODO: If Hytale uses different UV mapping, remap specific regions here
            // Example remapping logic (if needed):
            // remapHytaleRegions(hytaleImage, mcImage);
        } finally {
            g2d.dispose();
        }

        // Convert to byte array (PNG format)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(hytaleImage, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Translates a Hytale high-resolution skin down to Minecraft 64x64 format.
     * 
     * Strategy:
     * 1. Load the Hytale skin image
     * 2. Crop/scale down to 64x64 (Minecraft standard)
     * 3. Remap pixels according to Minecraft's UV layout if needed
     * 4. Return as byte array (PNG format)
     * 
     * @param hytaleSkin Raw image bytes (PNG format) of a Hytale skin
     * @return Byte array containing the Minecraft-compatible 64x64 skin image
     * @throws IOException If image processing fails
     */
    public byte[] translateHytaleToMc(byte[] hytaleSkin) throws IOException {
        if (hytaleSkin == null || hytaleSkin.length == 0) {
            throw new IllegalArgumentException("Hytale skin data is null or empty");
        }

        // Load Hytale skin image
        BufferedImage hytaleImage = ImageIO.read(new ByteArrayInputStream(hytaleSkin));
        if (hytaleImage == null) {
            throw new IOException("Failed to decode Hytale skin image");
        }

        logger.info(String.format("Translating Hytale skin: %dx%d -> MC 64x64 format",
                hytaleImage.getWidth(), hytaleImage.getHeight()));

        // Create Minecraft-sized image
        BufferedImage mcImage = new BufferedImage(
                MC_SKIN_WIDTH,
                MC_SKIN_HEIGHT,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = mcImage.createGraphics();
        try {
            // Enable high-quality downscaling
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Scale down the Hytale skin to Minecraft size
            // This will crop/scale the image to fit 64x64
            g2d.drawImage(hytaleImage, 0, 0, MC_SKIN_WIDTH, MC_SKIN_HEIGHT, null);

            // TODO: If Hytale uses different UV mapping, remap specific regions here
            // Example remapping logic (if needed):
            // remapMinecraftRegions(mcImage, hytaleImage);
        } finally {
            g2d.dispose();
        }

        // Convert to byte array (PNG format)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(mcImage, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Remaps specific regions from Minecraft to Hytale format.
     * 
     * Minecraft skin layout (64x64):
     * - Head: 8x8 at (8, 8)
     * - Body: 8x12 at (20, 20)
     * - Right Arm: 4x12 at (44, 20)
     * - Left Arm: 4x12 at (36, 52) [on second layer]
     * - Right Leg: 4x12 at (4, 20)
     * - Left Leg: 4x12 at (20, 52) [on second layer]
     * 
     * This method can be extended to handle specific UV remapping if Hytale
     * uses a different layout.
     */
    private void remapHytaleRegions(BufferedImage hytaleImage, BufferedImage mcImage) {
        // Example: If Hytale has different region positions, remap here
        // For now, we assume 2x scaling maintains the same layout
        
        // If Hytale uses different coordinates, you would do:
        // - Extract specific regions from mcImage
        // - Place them at different coordinates in hytaleImage
        // - Handle any rotation or mirroring if needed
    }

    /**
     * Remaps specific regions from Hytale to Minecraft format.
     * 
     * Crops and scales specific body parts if Hytale uses a different layout.
     */
    private void remapMinecraftRegions(BufferedImage mcImage, BufferedImage hytaleImage) {
        // Example: If Hytale has different region positions, remap here
        // Extract regions from hytaleImage and place them in correct MC positions
        
        // If Hytale uses different coordinates, you would do:
        // - Extract specific regions from hytaleImage
        // - Scale them down appropriately
        // - Place them at Minecraft's standard coordinates
    }

    /**
     * Helper method to extract a region from an image.
     * 
     * @param source Source image
     * @param x X coordinate of the region
     * @param y Y coordinate of the region
     * @param width Width of the region
     * @param height Height of the region
     * @return Extracted region as a new BufferedImage
     */
    private BufferedImage extractRegion(BufferedImage source, int x, int y, int width, int height) {
        // Ensure coordinates are within bounds
        x = Math.max(0, Math.min(x, source.getWidth() - 1));
        y = Math.max(0, Math.min(y, source.getHeight() - 1));
        width = Math.min(width, source.getWidth() - x);
        height = Math.min(height, source.getHeight() - y);

        BufferedImage region = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = region.createGraphics();
        g.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
        g.dispose();
        return region;
    }

    /**
     * Validates that a skin image has valid dimensions.
     * 
     * @param image The image to validate
     * @param expectedWidth Expected width
     * @param expectedHeight Expected height
     * @return True if dimensions match (or are close enough)
     */
    public boolean validateSkinDimensions(BufferedImage image, int expectedWidth, int expectedHeight) {
        if (image == null) {
            return false;
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Allow some tolerance for non-standard skins
        return Math.abs(width - expectedWidth) <= 2 && Math.abs(height - expectedHeight) <= 2;
    }
}
