package com.kynv1.telephonyandroid.telephony;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

/** Phase 1: thông tin SIM / subscription và việc app có carrier privilege hay không. */
public final class SimInfoProbe {

    private SimInfoProbe() {}

    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        SubscriptionScanner.forEach(ctx, r, (si, tm) -> describe(r, si, tm));
        return r;
    }

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
