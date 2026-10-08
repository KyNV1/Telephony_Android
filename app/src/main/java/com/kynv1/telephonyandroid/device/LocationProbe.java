package com.kynv1.telephonyandroid.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.kynv1.telephonyandroid.telephony.Permissions;
import com.kynv1.telephonyandroid.telephony.ProbeReport;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Phase 3: vị trí hiện tại và cờ "vị trí giả" (mock). Dùng LocationManager, không cần Play Services.
 * Mô phỏng phần client của luồng kiểm tra vị trí kênh bán: server chỉ tin được tọa độ nếu cờ mock
 * được kiểm tra. Báo cáo được đánh dấu nhạy cảm nên không ghi log tọa độ.
 * Phải gọi từ thread nền: hàm chờ vị trí tối đa 10 giây.
 */
public final class LocationProbe {

    private static final long TIMEOUT_SECONDS = 10;

    private LocationProbe() {}

    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport().sensitive();
        boolean fine = Permissions.has(ctx, Permissions.LOCATION);
        boolean coarse = Permissions.has(ctx, Permissions.LOCATION_COARSE);
        if (!fine && !coarse) {
            r.err(Permissions.missingMessage(Permissions.LOCATION));
            return r;
        }
        if (!fine) {
            r.warn("Chỉ có quyền vị trí gần đúng (COARSE): độ chính xác thấp và không dùng được GPS");
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.err("Không được chạy từ main thread");
            return r;
        }
        LocationManager lm = ctx.getSystemService(LocationManager.class);
        if (lm == null) {
            r.err("Không lấy được LocationManager");
            return r;
        }
        String provider = pickProvider(lm, fine);
        if (provider == null) {
            r.warn("Vị trí đang tắt trong Cài đặt hoặc không có nhà cung cấp nào dùng được");
            return r;
        }

        Location loc;
        try {
            loc = currentLocation(ctx, lm, provider);
        } catch (SecurityException e) {
            r.err(Permissions.missingMessage(Permissions.LOCATION));
            return r;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            r.err("Bị ngắt khi chờ vị trí");
            return r;
        }
        if (loc == null) {
            r.warn("Chưa lấy được vị trí sau " + TIMEOUT_SECONDS + "s (đang ở trong nhà hoặc chưa có tín hiệu)");
            return r;
        }
        describe(r, loc, provider);
        return r;
    }

    private static void describe(ProbeReport r, Location loc, String provider) {
        r.section("Vị trí");
        r.info("Nhà cung cấp: " + provider);
        r.info(String.format(Locale.US, "Tọa độ: %.5f, %.5f", loc.getLatitude(), loc.getLongitude()));
        if (loc.hasAccuracy()) {
            r.info(String.format(Locale.US, "Độ chính xác: ±%.0f m", loc.getAccuracy()));
        }
        double ageSeconds = (SystemClock.elapsedRealtimeNanos() - loc.getElapsedRealtimeNanos()) / 1e9;
        r.info(String.format(Locale.US, "Tuổi dữ liệu: %.0f giây", Math.max(0, ageSeconds)));
        if (isMock(loc)) {
            r.warn("Vị trí do app giả lập (mock) cung cấp, không đáng tin");
        } else {
            r.ok("Không phát hiện vị trí giả (nhưng điều này không chứng minh tuyệt đối vị trí là thật)");
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isMock(Location loc) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? loc.isMock() : loc.isFromMockProvider();
    }

    private static String pickProvider(LocationManager lm, boolean fine) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
            return LocationManager.FUSED_PROVIDER;
        }
        if (fine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) return LocationManager.GPS_PROVIDER;
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return LocationManager.NETWORK_PROVIDER;
        return null;
    }

    // Quyền đã được kiểm tra ở run(); SecurityException vẫn được bắt ở nơi gọi.
    @SuppressLint("MissingPermission")
    private static Location currentLocation(Context ctx, LocationManager lm, String provider)
            throws InterruptedException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return awaitCurrent(ctx, lm, provider);
        }
        return lm.getLastKnownLocation(provider);
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private static Location awaitCurrent(Context ctx, LocationManager lm, String provider)
            throws InterruptedException {
        CancellationSignal signal = new CancellationSignal();
        AtomicReference<Location> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        lm.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(ctx), location -> {
            result.set(location);
            latch.countDown();
        });
        if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            signal.cancel();
            return null;
        }
        return result.get();
    }
}
