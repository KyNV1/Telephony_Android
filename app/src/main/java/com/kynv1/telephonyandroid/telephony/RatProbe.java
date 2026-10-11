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

        /** Tạo kết quả thành công; giá trị -1 từ telephony được coi là không đọc được. */
        static Mask of(long value) {
            return value == NetworkTypeMasks.UNREADABLE ? failed("telephony trả -1") : new Mask(value, null);
        }

        /** Tạo kết quả không đọc được kèm lý do. */
        static Mask failed(String error) {
            return new Mask(NetworkTypeMasks.UNREADABLE, error);
        }

        /** @return true nếu đọc được mask (không có lỗi) */
        public boolean ok() {
            return error == null;
        }

        /** true/false nếu đọc được, null nếu không (để bộ chẩn đoán bỏ qua bước này). */
        public Boolean hasNr() {
            return ok() ? NetworkTypeMasks.hasNr(value) : null;
        }
    }

    /**
     * Điểm vào của probe RAT: với mỗi SIM đang hoạt động, in loại mạng dữ liệu hiện tại và các mask cho phép.
     *
     * @param ctx context của ứng dụng
     * @return báo cáo theo từng SIM, không bao giờ null
     */
    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        SubscriptionScanner.forEach(ctx, r, (si, tm) -> describe(r, tm));
        return r;
    }

    /**
     * Ghi loại mạng dữ liệu hiện tại và ba mask (USER, CARRIER, 2G) của một SIM.
     * Nếu cả ba đều không đọc được thì chỉ ghi một dòng bỏ qua kèm lý do.
     *
     * @param r  báo cáo để ghi kết quả
     * @param tm TelephonyManager đã gắn đúng SIM này
     */
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

    /** In một mask dưới dạng chữ (ví dụ "LTE 5G"), hoặc dòng bỏ qua kèm lý do nếu không đọc được. */
    private static void describeMask(ProbeReport r, String label, Mask m) {
        if (m.ok()) {
            r.info("Mask " + label + ": " + NetworkTypeMasks.decode(m.value));
        } else {
            r.skip("Mask " + label + ": " + m.error);
        }
    }

    /**
     * Đọc mask loại mạng được phép theo một "reason". App thường không có quyền này nên
     * SecurityException là kết quả bình thường và được đổi thành {@link Mask} không đọc được, không ném ra ngoài.
     *
     * @param tm     TelephonyManager đã gắn đúng SIM cần đọc
     * @param reason ALLOWED_NETWORK_TYPES_REASON_* ({@link #REASON_USER}, {@link #REASON_CARRIER}, {@link #REASON_2G})
     * @return mask hoặc lý do không đọc được (API dưới 31, thiếu quyền, lỗi khác)
     */
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

    /**
     * Đọc loại mạng dữ liệu hiện tại (NETWORK_TYPE_*).
     *
     * @param tm TelephonyManager đã gắn đúng SIM cần đọc
     * @return NETWORK_TYPE_*, hoặc NETWORK_TYPE_UNKNOWN nếu không đọc được
     */
    @SuppressLint("MissingPermission")
    public static int dataNetworkType(TelephonyManager tm) {
        try {
            return tm.getDataNetworkType();
        } catch (RuntimeException e) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }
}
