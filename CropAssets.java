
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.io.File;

public class CropAssets {
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File("file_000000001ca47209a899ccaac11656fa.png"));
        int w = src.getWidth();
        int h = src.getHeight();

        // 1. App Icon (Top card box: approx x=410, y=50, w=434, h=434)
        // Let's sample pixel colors to locate exact bounding boxes
        // Find top card
        System.out.println("Source: " + w + "x" + h);

        // Crop App Icon (1024, 512, 192)
        BufferedImage appIcon = crop(src, 400, 40, 454, 454);
        saveScaled(appIcon, 1024, 1024, "app_icon_1024.png");
        saveScaled(appIcon, 512, 512, "app_icon_512.png");
        saveScaled(appIcon, 192, 192, "app_icon_192.png");

        // Crop Brand Icon / Ring only (Top ring inside card or bottom ring)
        // In top card, ring is roughly x=450, y=90, w=354, h=354 inside card
        BufferedImage brandRing = crop(src, 450, 90, 354, 354);
        BufferedImage brandRingTrans = makeBlackTransparent(brandRing);
        saveScaled(brandRingTrans, 1024, 1024, "brand_icon_1024.png");

        // Notification Icon (96, 48, 32)
        saveScaled(brandRingTrans, 96, 96, "notif_icon_96.png");
        saveScaled(brandRingTrans, 48, 48, "notif_icon_48.png");
        saveScaled(brandRingTrans, 32, 32, "notif_icon_32.png");

        // AppBar Logo (Horizontal Ring + AIRA text) -> bottom left x=120, y=1070, w=270, h=90
        BufferedImage appBarLogo = crop(src, 120, 1070, 270, 90);
        BufferedImage appBarTrans = makeBlackTransparent(appBarLogo);
        saveScaled(appBarTrans, 200, 60, "appbar_logo_200x60.png");

        // Splash Screen (1080x1920) -> Centered logo/ring + AIRA + tagline on black background
        BufferedImage splash = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = splash.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1080, 1920);
        // Draw centered ring + text from splash box (x=390, y=730, w=160, h=270)
        BufferedImage splashContent = crop(src, 390, 730, 160, 270);
        int cw = 480;
        int ch = (int)(270.0 / 160.0 * cw);
        g.drawImage(splashContent, (1080 - cw)/2, (1920 - ch)/2, cw, ch, null);
        g.dispose();
        ImageIO.write(splash, "png", new File("splash_screen_1080x1920.png"));

        System.out.println("Assets cropped successfully.");
    }

    private static BufferedImage crop(BufferedImage src, int x, int y, int w, int h) {
        return src.getSubimage(x, y, w, h);
    }

    private static void saveScaled(BufferedImage src, int targetW, int targetH, String fileName) throws Exception {
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        ImageIO.write(out, "png", new File(fileName));
    }

    private static BufferedImage makeBlackTransparent(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // If pixel is black/dark background, make transparent smoothly
                int brightness = (r + g + b) / 3;
                if (brightness < 20) {
                    out.setRGB(x, y, 0x00000000);
                } else {
                    out.setRGB(x, y, rgb);
                }
            }
        }
        return out;
    }
}
