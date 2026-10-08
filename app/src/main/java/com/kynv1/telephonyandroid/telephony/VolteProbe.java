package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;

/**
 * Phase 4: VoLTE / IMS, chỉ đọc. Công tắc "Advanced calling" cần READ_PRIVILEGED_PHONE_STATE hoặc
 * READ_PRECISE_PHONE_STATE nên app thường không đọc được; phần carrier config (công khai) luôn in ra.
 */
public final class VolteProbe {

    private VolteProbe() {}

    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        SubscriptionScanner.forEach(ctx, r, (si, tm) -> describe(ctx, r, si.getSubscriptionId()));
        return r;
    }

    private static void describe(Context ctx, ProbeReport r, int subId) {
        CarrierConfigProbe.reportVolteKeys(ctx, subId, r);
        describeAdvancedCalling(ctx, r, subId);
        r.info("Lưu ý: công tắc bật chưa chắc VoLTE đang chạy; trạng thái đăng ký IMS cần READ_PRECISE_PHONE_STATE");
    }

    // Quyền này chỉ dành cho app đặc quyền; app thường thiếu quyền là kết quả dự kiến và được bắt bên dưới.
    @SuppressLint("MissingPermission")
    private static void describeAdvancedCalling(Context ctx, ProbeReport r, int subId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            r.skip("Công tắc Advanced calling: cần API 30 trở lên");
            return;
        }
        if (!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS)) {
            r.skip("Công tắc Advanced calling: thiết bị không có tính năng IMS");
            return;
        }
        ImsManager im = ctx.getSystemService(ImsManager.class);
        if (im == null) {
            r.skip("Công tắc Advanced calling: không lấy được ImsManager");
            return;
        }
        try {
            ImsMmTelManager mmtel = im.getImsMmTelManager(subId);
            boolean enabled = mmtel.isAdvancedCallingSettingEnabled();
            r.info("Công tắc Advanced calling (VoLTE): " + (enabled ? "BẬT" : "TẮT"));
        } catch (SecurityException e) {
            r.skip("Công tắc Advanced calling: cần READ_PRIVILEGED_PHONE_STATE hoặc READ_PRECISE_PHONE_STATE "
                    + "(app thường không có)");
        } catch (RuntimeException e) {
            r.err("Công tắc Advanced calling: " + ProbeReport.rootCause(e));
        }
    }
}
