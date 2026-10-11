package com.kynv1.telephonyandroid.telephony;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

/** Phase 1: thông tin SIM / subscription và việc app có carrier privilege hay không. */
public final class SimInfoProbe {

    private SimInfoProbe() {}

    /**
     * Điểm vào của probe SIM: duyệt từng SIM đang hoạt động và ghi thông tin của SIM đó vào báo cáo.
     * Nếu không duyệt được (không có telephony, thiếu quyền, không có SIM) thì lý do đã được
     * {@link SubscriptionScanner} ghi sẵn vào báo cáo. Phải gọi từ thread nền.
     *
     * @param ctx context của ứng dụng, dùng để lấy các dịch vụ hệ thống
     * @return báo cáo chứa thông tin từng SIM, không bao giờ null
     */
    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        SubscriptionScanner.forEach(ctx, r, (si, tm) -> describe(r, si, tm));
        return r;
    }

    /**
     * Ghi thông tin của một SIM vào báo cáo: tên hiển thị, quốc gia (kèm MCC-MNC từ API 29),
     * eSIM hay không (từ API 28) và app có carrier privilege trên SIM này hay không.
     * Không có carrier privilege chỉ là thông tin vì app thường không bao giờ có; chỉ khi
     * việc kiểm tra ném lỗi mới ghi mức lỗi.
     *
     * @param r  báo cáo để ghi các dòng kết quả
     * @param si thông tin subscription của SIM đang xét
     * @param tm TelephonyManager đã gắn đúng SIM này
     */
    private static void describe(ProbeReport r, SubscriptionInfo si, TelephonyManager tm) {
        r.info("Tên hiển thị: " + si.getDisplayName());
        String mccMnc = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mccMnc = "  MCC-MNC: " + si.getMccString() + "-" + si.getMncString();
        }
        r.info("Quốc gia: " + si.getCountryIso() + mccMnc);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.info("eSIM: " + si.isEmbedded());
        }
        try {
            if (tm.hasCarrierPrivileges()) {
                r.ok("Có carrier privilege trên SIM này (các thao tác đặc quyền có thể dùng được)");
            } else {
                r.info("Không có carrier privilege: bình thường với app thường; "
                        + "APK phải ký khớp chứng chỉ ghi trên SIM mới có");
            }
        } catch (RuntimeException e) {
            r.err("Không kiểm tra được carrier privilege: " + ProbeReport.rootCause(e));
        }
    }
}
