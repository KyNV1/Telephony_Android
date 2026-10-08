package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Đọc "override network type" của {@link TelephonyDisplayInfo}: đây là loại mạng mà thanh trạng thái
 * hiển thị (ví dụ NR_NSA = biểu tượng 5G). Là API công khai chỉ cần READ_PHONE_STATE, nên app thường
 * dùng được khi không đọc được NR state ẩn hay mask đặc quyền.
 * Phải gọi từ thread nền: hàm chờ callback tối đa 2 giây.
 */
public final class DisplayInfoReader {

    private static final long TIMEOUT_SECONDS = 2;

    private DisplayInfoReader() {}

    public static final class Result {
        public final int overrideType;
        public final String error;

        Result(int overrideType, String error) {
            this.overrideType = overrideType;
            this.error = error;
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Callback extends TelephonyCallback
            implements TelephonyCallback.DisplayInfoListener {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile int overrideType = FiveGDiagnoser.OVERRIDE_UNKNOWN;

        @Override
        public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo info) {
            overrideType = info.getOverrideNetworkType();
            latch.countDown();
        }
    }

    @SuppressLint("MissingPermission")
    public static Result read(Context ctx, TelephonyManager tm) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return unavailable("cần API 31 trở lên");
        }
        if (!Permissions.has(ctx, Permissions.PHONE)) {
            return unavailable(Permissions.missingMessage(Permissions.PHONE));
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return unavailable("không được gọi từ main thread");
        }
        Callback cb = new Callback();
        try {
            tm.registerTelephonyCallback(ContextCompat.getMainExecutor(ctx), cb);
            if (!cb.latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return unavailable("hết " + TIMEOUT_SECONDS + "s mà chưa nhận callback");
            }
            return new Result(cb.overrideType, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return unavailable("bị ngắt khi chờ callback");
        } catch (RuntimeException e) {
            return unavailable(ProbeReport.rootCause(e));
        } finally {
            try {
                tm.unregisterTelephonyCallback(cb);
            } catch (RuntimeException ignored) {
                // chưa đăng ký được thì không có gì để hủy
            }
        }
    }

    private static Result unavailable(String reason) {
        return new Result(FiveGDiagnoser.OVERRIDE_UNKNOWN, reason);
    }
}
