package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.List;

/**
 * Lặp qua từng SIM đang hoạt động và đưa cho probe một {@link TelephonyManager} gắn đúng SIM đó.
 * Mọi tình huống không thể lặp (không có telephony, thiếu quyền, không SIM) đều được ghi vào
 * báo cáo kèm lý do, không bỏ qua im lặng.
 */
public final class SubscriptionScanner {

    public interface Visitor {
        void visit(SubscriptionInfo si, TelephonyManager tm);
    }

    private SubscriptionScanner() {}

    // READ_PHONE_STATE đã được kiểm tra bằng Permissions.has ngay bên dưới; SecurityException vẫn được bắt.
    @SuppressLint("MissingPermission")
    public static void forEach(Context ctx, ProbeReport r, Visitor visitor) {
        if (!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            r.skip("Thiết bị không có tính năng telephony (emulator hoặc tablet chỉ Wi-Fi)");
            return;
        }
        if (!Permissions.has(ctx, Permissions.PHONE)) {
            r.err(Permissions.missingMessage(Permissions.PHONE));
            return;
        }
        TelephonyManager base = ctx.getSystemService(TelephonyManager.class);
        SubscriptionManager sm = ctx.getSystemService(SubscriptionManager.class);
        if (base == null || sm == null) {
            r.err("Không lấy được TelephonyManager/SubscriptionManager từ hệ thống");
            return;
        }

        List<SubscriptionInfo> subs;
        try {
            subs = sm.getActiveSubscriptionInfoList();
        } catch (SecurityException e) {
            r.err("Không đọc được danh sách SIM: " + ProbeReport.rootCause(e));
            return;
        }
        if (subs == null || subs.isEmpty()) {
            r.warn("Không có SIM nào đang hoạt động (chưa lắp SIM, chế độ máy bay hoặc SIM chưa kích hoạt)");
            return;
        }

        for (SubscriptionInfo si : subs) {
            r.section("SIM khe " + si.getSimSlotIndex() + " | subId " + si.getSubscriptionId()
                    + " | " + si.getCarrierName());
            TelephonyManager tm;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tm = base.createForSubscriptionId(si.getSubscriptionId());
            } else {
                tm = base;
                if (subs.size() > 1) {
                    r.warn("API < 26 không tách được từng SIM: số liệu bên dưới là của SIM mặc định");
                }
            }
            visitor.visit(si, tm);
        }
    }
}
