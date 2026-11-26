package view.fx;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hook âm thanh SFX siêu nhẹ dùng javax.sound.sampled (không cần javafx.media).
 * Chạy trên luồng daemon riêng nên không bao giờ làm khựng game loop; tự động bỏ qua nếu máy tắt âm.
 */
public final class SoundHook {

    private static volatile boolean enabled = true;
    private static volatile boolean bgmEnabled = true;
    private static volatile boolean bgmRunning = false;
    private static volatile double masterVolume = 0.80; // 0.0 .. 1.0 (80% mặc định)
    private static Thread bgmThread = null;
    private static final float SAMPLE_RATE = 22050f;

    private static final ExecutorService AUDIO_EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SmartFarm-SFX");
        t.setDaemon(true);
        return t;
    });

    private SoundHook() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isBgmEnabled() {
        return bgmEnabled;
    }

    public static void setBgmEnabled(boolean value) {
        bgmEnabled = value;
        if (!value) {
            stopMenuBgm();
        }
    }

    public static double getMasterVolume() {
        return masterVolume;
    }

    public static void setMasterVolume(double value) {
        if (Double.isNaN(value)) return;
        masterVolume = Math.max(0.0, Math.min(1.0, value));
    }

    /** Tiếng pop pixel rất nhẹ khi di chuột qua nút gỗ trên menu. */
    public static void playMenuHoverPop() {
        playSweep(620.0, 920.0, 26, 0.065);
    }

    /** Tiếng gõ gỗ ngắn gọn khi nhấn nút menu. */
    public static void playMenuClick() {
        playToneSequence(new double[][] {
            { 440.0, 24, 0.11 },
            { 660.0, 34, 0.09 }
        });
    }

    /** Âm thanh mở cổng gỗ ấm áp + hợp âm khởi hành khi bước vào nông trại. */
    public static void playWoodGateOpen() {
        if (!enabled) return;
        AUDIO_EXEC.submit(() -> {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                line.open(fmt);
                line.start();
                // Tiếng cọt kẹt bản lề gỗ trầm ấm + 3 nốt hợp âm nông trại (C5 - E5 - G5)
                writeSine(line, 195.0, 285.0, 120, 0.13);
                writeSine(line, 260.0, 220.0, 90, 0.10);
                writeSine(line, 523.25, 523.25, 95, 0.11);
                writeSine(line, 659.25, 659.25, 95, 0.11);
                writeSine(line, 783.99, 783.99, 180, 0.12);
                line.drain();
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Tự động phát nhạc nền 8-bit nông trại vui tươi, thư giãn trên luồng nền riêng.
     */
    public static synchronized void startMenuBgm() {
        if (!bgmEnabled || bgmRunning) return;
        bgmRunning = true;
        bgmThread = new Thread(() -> {
            // Giai điệu đồng quê 8-bit vui vẻ giọng Đô trưởng (C Major) — êm dịu, không chói tai
            final double[] melody = {
                523.25, 659.25, 783.99, 659.25, 880.00, 783.99, 659.25, 587.33,
                523.25, 587.33, 659.25, 523.25, 440.00, 523.25, 587.33, 0.0,
                659.25, 783.99, 880.00, 783.99, 659.25, 587.33, 523.25, 587.33,
                659.25, 587.33, 523.25, 440.00, 523.25, 0.0,    392.00, 0.0
            };
            final double[] bass = {
                261.63, 261.63, 329.63, 329.63, 220.00, 220.00, 196.00, 196.00,
                174.61, 174.61, 261.63, 261.63, 174.61, 174.61, 196.00, 196.00,
                261.63, 261.63, 220.00, 220.00, 174.61, 174.61, 196.00, 196.00,
                174.61, 196.00, 261.63, 220.00, 261.63, 261.63, 196.00, 196.00
            };
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                line.open(fmt);
                line.start();
                int step = 0;
                while (bgmRunning && bgmEnabled) {
                    double mHz = melody[step % melody.length];
                    double bHz = bass[step % bass.length];
                    writeChiptuneBeat(line, mHz, bHz, 270, 0.045);
                    step++;
                }
                line.stop();
                line.flush();
            } catch (Exception ignored) {
            } finally {
                bgmRunning = false;
            }
        }, "SmartFarm-MenuBGM");
        bgmThread.setDaemon(true);
        bgmThread.start();
    }

    public static synchronized void stopMenuBgm() {
        bgmRunning = false;
        if (bgmThread != null) {
            bgmThread.interrupt();
            bgmThread = null;
        }
    }

    private static void writeChiptuneBeat(SourceDataLine line, double melodyHz, double bassHz, int ms, double volume) {
        int samples = (int) (SAMPLE_RATE * ms / 1000.0);
        byte[] buf = new byte[samples * 2];
        double pM = 0.0;
        double pB = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / Math.max(1, samples - 1);
            double env = (t < 0.08) ? (t / 0.08) : (t > 0.72 ? (1.0 - t) / 0.28 : 1.0);
            double sig = 0.0;
            if (melodyHz > 1.0) {
                pM += 2.0 * Math.PI * melodyHz / SAMPLE_RATE;
                // Sóng tam giác pha sine ấm áp kiểu nhạc đồng quê 8-bit
                double tri = (2.0 / Math.PI) * Math.asin(Math.sin(pM));
                sig += (tri * 0.65 + Math.sin(pM) * 0.35) * 0.72;
            }
            if (bassHz > 1.0) {
                pB += 2.0 * Math.PI * bassHz / SAMPLE_RATE;
                sig += Math.sin(pB) * 0.38;
            }
            short val = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sig * Short.MAX_VALUE * volume * masterVolume * env));
            buf[2 * i]     = (byte) (val & 0xFF);
            buf[2 * i + 1] = (byte) ((val >> 8) & 0xFF);
        }
        line.write(buf, 0, buf.length);
    }

    /** Tiếng nhặt vật phẩm sáng nhẹ (2 nốt tăng dần C6 -> G6). */
    public static void playPickup() {
        playToneSequence(new double[][] {
            { 1046.5, 42, 0.14 },
            { 1568.0, 62, 0.12 }
        });
    }

    /** Tiếng thu hoạch vui tai (G5 -> B5). */
    public static void playHarvest() {
        playToneSequence(new double[][] {
            { 784.0, 40, 0.13 },
            { 987.8, 52, 0.11 }
        });
    }

    /** Tiếng giọt nước mềm khi tưới cây. */
    public static void playWaterSplash() {
        playSweep(520.0, 880.0, 68, 0.10);
    }

    /** Tiếng bật trái tim ấm áp khi vuốt ve / cho ăn vật nuôi. */
    public static void playHeart() {
        playToneSequence(new double[][] {
            { 659.3, 45, 0.12 },
            { 880.0, 65, 0.11 }
        });
    }

    /** Tiếng giật dây câu nhẹ khi spam phím Space (Play Together style). */
    public static void playReelTap(double progress) {
        double pitch = 520.0 + Math.min(1.0, Math.max(0.0, progress)) * 360.0;
        playSweep(pitch, pitch + 110.0, 28, 0.08);
    }

    /** Tiếng nhấp mồi giả rất nhẹ (plop nước). */
    public static void playNibblePlop() {
        playSweep(340.0, 480.0, 40, 0.07);
    }

    /** Tiếng báo hiệu cắn thật "!" sắc nét để kích hoạt phản xạ giật cần. */
    public static void playBiteAlert() {
        playToneSequence(new double[][] {
            { 1174.7, 50, 0.16 },
            { 1760.0, 85, 0.16 }
        });
    }

    private static void playToneSequence(double[][] notes) {
        if (!enabled) return;
        AUDIO_EXEC.submit(() -> {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                line.open(fmt);
                line.start();
                for (double[] note : notes) {
                    writeSine(line, note[0], note[0], (int) note[1], note[2]);
                }
                line.drain();
            } catch (Exception ignored) {
                // Không làm gián đoạn game nếu không có thiết bị âm thanh
            }
        });
    }

    private static void playSweep(double startHz, double endHz, int ms, double volume) {
        if (!enabled) return;
        AUDIO_EXEC.submit(() -> {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                line.open(fmt);
                line.start();
                writeSine(line, startHz, endHz, ms, volume);
                line.drain();
            } catch (Exception ignored) {
            }
        });
    }

    private static void writeSine(SourceDataLine line, double f0, double f1, int ms, double volume) {
        int samples = (int) (SAMPLE_RATE * ms / 1000.0);
        byte[] buf = new byte[samples * 2];
        double phase = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / Math.max(1, samples - 1);
            double freq = f0 + (f1 - f0) * t;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            // Envelope mượt (fade-in 12%, fade-out 35%) tránh tiếng "tách"
            double env = 1.0;
            if (t < 0.12) env = t / 0.12;
            else if (t > 0.65) env = (1.0 - t) / 0.35;
            short val = (short) (Math.sin(phase) * Short.MAX_VALUE * volume * masterVolume * env);
            buf[2 * i]     = (byte) (val & 0xFF);
            buf[2 * i + 1] = (byte) ((val >> 8) & 0xFF);
        }
        line.write(buf, 0, buf.length);
    }
}
