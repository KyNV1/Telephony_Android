package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;

/**
 * Logic thuần trả lời "vì sao máy chưa lên 5G". Không gọi dịch vụ hệ thống nên test được.
 * Thứ tự kiểm tra: đang 5G -> mask user -> mask carrier -> carrier config -> neo LTE
 * -> cell hạn chế NR -> không đọc được NR state -> vùng phủ.
 */
@SuppressLint("InlinedApi")
public final class FiveGDiagnoser {

    // NR state theo NetworkRegistrationInfo (API 36): NONE=0, RESTRICTED=1, NOT_RESTRICTED=2, CONNECTED=3.
    public static final int NR_UNREADABLE = -1;
    public static final int NR_NONE = 0;
    public static final int NR_RESTRICTED = 1;
    public static final int NR_NOT_RESTRICTED = 2;
    public static final int NR_CONNECTED = 3;

    public static final int OVERRIDE_UNKNOWN = -1;

    public static final String CFG_NONE = "none";
    public static final String CFG_NA = "n/a";
    public static final String CFG_ERR = "err";

    public enum Verdict {
        ON_5G("Đang dùng 5G"),
        USER_MASK_NO_NR("Mask 'user' không có NR (cài đặt loại mạng trên máy)"),
        CARRIER_MASK_NO_NR("Mask 'carrier' không có NR (cần khôi phục về AUTO)"),
        CONFIG_NO_NR("Carrier config không cho phép NR"),
        NO_LTE_ANCHOR("Mất neo LTE: loại mạng đang bị ép sang công nghệ khác"),
        NR_RESTRICTED_BY_CELL("Cell hạn chế NR"),
        NR_STATE_UNREADABLE("Không đọc được NR state; neo LTE vẫn ổn"),
        NO_NR_COVERAGE("Không có cell NR trong vùng phủ");

        public final String description;

        Verdict(String description) {
            this.description = description;
        }
    }

    private FiveGDiagnoser() {
    }

    /**
     * @param userNr       mask USER có bit NR; null nếu không đọc được (thiếu quyền đặc quyền)
     * @param carrierNr    mask CARRIER có bit NR; null nếu không đọc được
     * @param cfgNr        chuỗi từ {@link #formatNrAvailabilities(int[])} hoặc CFG_NA / CFG_ERR
     * @param rat          NETWORK_TYPE_* hiện tại của dữ liệu
     * @param nrState      NR_* ở trên, NR_UNREADABLE nếu không đọc được
     * @param overrideType TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_*, OVERRIDE_UNKNOWN nếu không có
     */
    public static Verdict verdict(Boolean userNr, Boolean carrierNr, String cfgNr,
                                  int rat, int nrState, int overrideType) {
        if (nrState == NR_CONNECTED || rat == TelephonyManager.NETWORK_TYPE_NR || isNrOverride(overrideType)) {
            return Verdict.ON_5G;
        }
        if (Boolean.FALSE.equals(userNr)) return Verdict.USER_MASK_NO_NR;
        if (Boolean.FALSE.equals(carrierNr)) return Verdict.CARRIER_MASK_NO_NR;
        if (CFG_NONE.equals(cfgNr)) return Verdict.CONFIG_NO_NR;
        if (rat != TelephonyManager.NETWORK_TYPE_LTE && rat != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            return Verdict.NO_LTE_ANCHOR;
        }
        if (nrState == NR_RESTRICTED) return Verdict.NR_RESTRICTED_BY_CELL;
        if (nrState < 0) return Verdict.NR_STATE_UNREADABLE;
        return Verdict.NO_NR_COVERAGE;
    }

    public static boolean isNrOverride(int overrideType) {
        return overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
                || overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE
                || overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED;
    }

    /**
     * {NSA, SA} -> "NSA+SA"; rỗng/null -> "none" (nhà mạng không hỗ trợ NR).
     */
    public static String formatNrAvailabilities(int[] availabilities) {
        if (availabilities == null || availabilities.length == 0) return CFG_NONE;
        StringBuilder o = new StringBuilder();
        for (int a : availabilities) {
            if (o.length() > 0) o.append('+');
            if (a == CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA) o.append("SA");
            else if (a == CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA) o.append("NSA");
            else o.append(a);
        }
        return o.toString();
    }

    public static String nrName(int s) {
        switch (s) {
            case NR_NONE:
                return "NONE";
            case NR_RESTRICTED:
                return "RESTRICTED";
            case NR_NOT_RESTRICTED:
                return "NOT_RESTRICTED";
            case NR_CONNECTED:
                return "CONNECTED";
            default:
                return "n/a";
        }
    }

    public static String overrideName(int type) {
        switch (type) {
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE:
                return "NONE";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA:
                return "LTE_CA";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO:
                return "LTE_ADVANCED_PRO";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA:
                return "NR_NSA";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE:
                return "NR_NSA_MMWAVE";
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED:
                return "NR_ADVANCED";
            case OVERRIDE_UNKNOWN:
                return "n/a";
            default:
                return String.valueOf(type);
        }
    }
}
