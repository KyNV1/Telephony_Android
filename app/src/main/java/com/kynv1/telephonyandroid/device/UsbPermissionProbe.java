package com.kynv1.telephonyandroid.device;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.kynv1.telephonyandroid.telephony.ProbeReport;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Phase 4: xin quyền truy cập thiết bị USB đầu tiên chưa có quyền, rồi chờ kết quả.
 * Hệ thống thêm dữ liệu vào PendingIntent nên PendingIntent phải mutable (từ Android 12); vì mutable
 * nên intent phải tường minh (có package), nếu không Android 14+ sẽ ném IllegalArgumentException.
 * Phải gọi từ thread nền: hàm chờ người dùng bấm hộp thoại tối đa 20 giây.
 */
public final class UsbPermissionProbe {

    private static final long TIMEOUT_SECONDS = 20;

    private UsbPermissionProbe() {}

    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.err("Không được chạy từ main thread");
            return r;
        }
        UsbManager um = ctx.getSystemService(UsbManager.class);
        if (um == null) {
            r.err("Không lấy được UsbManager");
            return r;
        }
        UsbDevice target = null;
        for (UsbDevice d : um.getDeviceList().values()) {
            if (!um.hasPermission(d)) {
                target = d;
                break;
            }
        }
        if (target == null) {
            r.info("Không có thiết bị nào cần xin quyền (không cắm thiết bị, hoặc đều đã có quyền)");
            return r;
        }

        String action = ctx.getPackageName() + ".USB_PERMISSION";
        AtomicReference<Boolean> granted = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (action.equals(intent.getAction())) {
                    granted.set(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
                    latch.countDown();
                }
            }
        };
        ContextCompat.registerReceiver(ctx, receiver, new IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED);
        try {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                    | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
            Intent intent = new Intent(action).setPackage(ctx.getPackageName());
            um.requestPermission(target, PendingIntent.getBroadcast(ctx, 0, intent, flags));
            r.info("Đã mở hộp thoại xin quyền cho " + target.getDeviceName() + ", chờ phản hồi...");
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                r.warn("Không nhận được phản hồi sau " + TIMEOUT_SECONDS + "s");
            } else if (Boolean.TRUE.equals(granted.get())) {
                r.ok("Người dùng đã cấp quyền truy cập thiết bị USB");
            } else {
                r.warn("Người dùng từ chối quyền truy cập thiết bị USB");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            r.err("Bị ngắt khi chờ phản hồi");
        } catch (RuntimeException e) {
            r.err("Không xin được quyền USB: " + ProbeReport.rootCause(e));
        } finally {
            ctx.unregisterReceiver(receiver);
        }
        return r;
    }
}
