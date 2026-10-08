package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

/**
 * Phase 2: công nghệ truy cập vô tuyến (RAT) hiện tại và mask loại mạng được phép theo từng "reason".
 * Chỉ đọc. Đọc mask cần READ_PRIVILEGED_PHONE_STATE hoặc carrier privilege nên với app thường
 * kết quả là "bỏ qua kèm lý do", không phải lỗi.
 */
@SuppressLint("InlinedApi")
public final class RatProbe {

    public static final int REASON_USER = TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER;
    public static final int REASON_CARRIER = TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER;
    /** ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G là @SystemApi nên dùng số 3 trực tiếp. */
    public static final int REASON_2G = 3;

    private RatProbe() {}

    /** Kết quả đọc một mask: hoặc có giá trị, hoặc có lý do không đọc được. */
    public static final class Mask {
        public final long value;
        public final String error;

        private Mask(long value, String error) {
            this.value = value;
            this.error = error;
        }

        static Mask of(long value) {
            return value == NetworkTypeMasks.UNREADABLE ? failed("telephony trả -1") : new Mask(value, null);
        }

        static Mask failed(String error) {
            return new Mask(NetworkTypeMasks.UNREADABLE, error);
        }

        public boolean ok() {
            return error == null;
        }

        /** true/false nếu đọc được, null nếu không (để bộ chẩn đoán bỏ qua bước này). */
        public Boolean hasNr() {
            return ok() ? NetworkTypeMasks.hasNr(value) : null;
        }
    }

    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        SubscriptionScanner.forEach(ctx, r, (si, tm) -> describe(r, tm));
        return r;
    }

    private static void describe(ProbeReport r, TelephonyManager tm) {
        int rat = dataNetworkType(tm);
        r.info("Loại mạng dữ liệu hiện tại: " + NetworkTypeMasks.ratName(rat) + " (NETWORK_TYPE=" + rat + ")");

        Mask user = readMask(tm, REASON_USER);
        Mask carrier = readMask(tm, REASON_CARRIER);
        Mask g2 = readMask(tm, REASON_2G);
        if (!user.ok() && !carrier.ok() && !g2.ok()) {
            r.skip("Không đọc được mask loại mạng được phép: " + user.error);
            return;
        }
        describeMask(r, "USER (cài đặt người dùng)", user);
        describeMask(r, "CARRIER (nhà mạng)", carrier);
        if (g2.ok()) {
            r.info("2G đang được phép: " + NetworkTypeMasks.has2g(g2.value));
        } else {
            r.skip("Mask 2G: " + g2.error);
        }
    }

    private static void describeMask(ProbeReport r, String label, Mask m) {
        if (m.ok()) {
            r.info("Mask " + label + ": " + NetworkTypeMasks.decode(m.value));
        } else {
            r.skip("Mask " + label + ": " + m.error);
        }
    }

    // Gọi trong try/catch SecurityException: app thường không có quyền này và đó là kết quả bình thường.
    @SuppressLint("MissingPermission")
    public static Mask readMask(TelephonyManager tm, int reason) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return Mask.failed("cần API 31 trở lên");
        }
        try {
            return Mask.of(tm.getAllowedNetworkTypesForReason(reason));
        } catch (SecurityException e) {
            return Mask.failed("cần READ_PRIVILEGED_PHONE_STATE hoặc carrier privilege (app thường không có)");
        } catch (RuntimeException e) {
            return Mask.failed(ProbeReport.rootCause(e));
        }
    }

    @SuppressLint("MissingPermission")
    public static int dataNetworkType(TelephonyManager tm) {
        try {
            return tm.getDataNetworkType();
        } catch (RuntimeException e) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }
}
