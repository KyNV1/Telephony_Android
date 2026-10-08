package com.kynv1.telephonyandroid.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import com.kynv1.telephonyandroid.telephony.ProbeReport;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Locale;
import java.util.UUID;

/**
 * Phase 1: danh tính thiết bị. Cho thấy ID nào còn dùng được (ANDROID_ID, UUID tự sinh) và ID nào
 * đã bị hệ thống hạn chế (serial, MAC), tức là lý do các hằng số cứng kiểu SERIAL_DEVICE_HARDCODE
 * hay MAC_ADDRESS trong app Bitel gốc là cách làm cũ.
 */
public final class DeviceIdProbe {

    private static final String PREFS = "device_probe";
    private static final String KEY_INSTALL_ID = "install_id";

    private DeviceIdProbe() {}

    // Cố ý đọc các ID này để minh họa: serial cần quyền đặc quyền nên app thường nhận SecurityException
    // (được bắt bên dưới). Báo cáo được đánh dấu nhạy cảm nên không bị ghi log.
    @SuppressLint({"HardwareIds", "MissingPermission"})
    @SuppressWarnings("deprecation")
    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport().sensitive();

        r.section("Thiết bị");
        r.info("Hãng / model: " + Build.MANUFACTURER + " " + Build.MODEL);
        r.info("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");

        r.section("Định danh còn dùng được");
        String androidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        r.info("ANDROID_ID: " + androidId);
        r.info("    đổi theo (người dùng + khóa ký app + thiết bị), mất khi khôi phục cài đặt gốc");
        r.info("ID cài đặt (UUID): " + installId(ctx));
        r.info("    do app tự sinh, mất khi gỡ app hoặc xóa dữ liệu; không dùng để nhận diện người");

        r.section("Định danh bị hạn chế (cách làm cũ)");
        try {
            String serial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? Build.getSerial() : Build.SERIAL;
            r.warn("Serial: " + serial + " (đọc được trên máy này, nhưng không nên dựa vào)");
        } catch (SecurityException e) {
            r.skip("Serial (Build.getSerial): bị chặn, cần READ_PRIVILEGED_PHONE_STATE");
        }
        reportMac(r);
        return r;
    }

    private static void reportMac(ProbeReport r) {
        try {
            NetworkInterface wlan = NetworkInterface.getByName("wlan0");
            if (wlan == null) {
                r.skip("MAC wlan0: không thấy giao diện này (Wi-Fi tắt hoặc bị ẩn)");
                return;
            }
            byte[] mac = wlan.getHardwareAddress();
            if (mac == null) {
                r.skip("MAC wlan0: hệ thống không cho app đọc (Android 11 trở lên)");
                return;
            }
            r.warn("MAC wlan0: " + format(mac) + " (nếu đọc được cũng có thể là MAC ngẫu nhiên, không phải MAC phần cứng)");
        } catch (SocketException e) {
            r.err("MAC wlan0: " + ProbeReport.rootCause(e));
        }
    }

    static String format(byte[] mac) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format(Locale.US, "%02X", mac[i]));
        }
        return sb.toString();
    }

    private static String installId(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = sp.getString(KEY_INSTALL_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            sp.edit().putString(KEY_INSTALL_ID, id).apply();
        }
        return id;
    }
}
