package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

/**
 * Phase 3/4: đọc carrier config theo từng SIM. Đọc cần READ_PHONE_STATE (hoặc carrier privilege),
 * khác với ghi (overrideConfig) vốn cần MODIFY_PHONE_STATE.
 */
@SuppressLint("InlinedApi")
public final class CarrierConfigProbe {

    private static final String[] VOLTE_LABELS = {
            "VoLTE khả dụng (carrier_volte_available)",
            "Enhanced 4G LTE bật mặc định",
            "Ẩn mục Enhanced 4G LTE",
            "Cho phép chỉnh Enhanced 4G LTE",
    };
    private static final String[] VOLTE_KEYS = {
            CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL,
            CarrierConfigManager.KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL,
            CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL,
            CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL,
    };

    private CarrierConfigProbe() {
    }

    /**
     * Điểm vào của probe carrier config: với mỗi SIM, in khả năng hỗ trợ NR (5G) và 4 khóa VoLTE.
     *
     * @param ctx context của ứng dụng
     * @return báo cáo theo từng SIM, không bao giờ null
     */
    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        SubscriptionScanner.forEach(ctx, r, (si, tm) -> {
            int subId = si.getSubscriptionId();
            r.info("Hỗ trợ NR (5G): " + nrConfigText(ctx, subId)
                    + "  (NSA cần neo LTE, SA là 5G độc lập, none = nhà mạng không bật NR)");
            reportVolteKeys(ctx, subId, r);
        });
        return r;
    }

    /**
     * In 4 khóa liên quan VoLTE; dùng chung cho probe carrier config và probe VoLTE.
     */
    static void reportVolteKeys(Context ctx, int subId, ProbeReport r) {
        PersistableBundle b = read(ctx, subId, r, VOLTE_KEYS);
        if (b == null) return;
        for (int i = 0; i < VOLTE_KEYS.length; i++) {
            String value = b.containsKey(VOLTE_KEYS[i]) ? String.valueOf(b.getBoolean(VOLTE_KEYS[i])) : "(không có)";
            r.info(VOLTE_LABELS[i] + ": " + value);
        }
    }

    /**
     * "NSA+SA", "none", hoặc FiveGDiagnoser.CFG_NA / CFG_ERR khi không đọc được.
     */
    static String nrConfigText(Context ctx, int subId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return FiveGDiagnoser.CFG_NA;
        PersistableBundle b = read(ctx, subId, null, CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        if (b == null) return FiveGDiagnoser.CFG_ERR;
        int[] av = b.getIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        return av == null ? FiveGDiagnoser.CFG_NA : FiveGDiagnoser.formatNrAvailabilities(av);
    }

    /**
     * Đọc carrier config của một SIM. Trả null nếu không đọc được; khi có {@code r} thì ghi lý do vào đó.
     */
    @SuppressWarnings("deprecation")
    private static PersistableBundle read(Context ctx, int subId, ProbeReport r, String... keys) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (r != null) r.skip("Carrier config theo subId cần API 26 trở lên");
            return null;
        }
        CarrierConfigManager cm = ctx.getSystemService(CarrierConfigManager.class);
        if (cm == null) {
            if (r != null) r.err("Không lấy được CarrierConfigManager");
            return null;
        }
        try {
            PersistableBundle b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? cm.getConfigForSubId(subId, keys)
                    : cm.getConfigForSubId(subId);
            if (b == null && r != null) r.skip("Hệ thống chưa nạp carrier config cho SIM này");
            return b;
        } catch (SecurityException e) {
            if (r != null) r.err(Permissions.missingMessage(Permissions.PHONE));
            return null;
        } catch (RuntimeException e) {
            if (r != null) r.err("Không đọc được carrier config: " + ProbeReport.rootCause(e));
            return null;
        }
    }
}
