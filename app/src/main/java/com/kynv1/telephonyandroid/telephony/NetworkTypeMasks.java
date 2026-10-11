package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.telephony.TelephonyManager;

/**
 * Mask loại mạng (mỗi bit là một công nghệ vô tuyến) và hàm đổi sang chữ.
 * Các hằng này được trình biên dịch "nhúng" thẳng giá trị nên dùng được trong unit test thuần.
 */
@SuppressLint("InlinedApi")
public final class NetworkTypeMasks {

    public static final long M_2G =
            TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                    | TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
                    | TelephonyManager.NETWORK_TYPE_BITMASK_EDGE;

    public static final long M_3G =
            TelephonyManager.NETWORK_TYPE_BITMASK_UMTS
                    | TelephonyManager.NETWORK_TYPE_BITMASK_HSPA
                    | TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA
                    | TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA
                    | TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP
                    | TelephonyManager.NETWORK_TYPE_BITMASK_TD_SCDMA;

    public static final long M_LTE = TelephonyManager.NETWORK_TYPE_BITMASK_LTE;
    public static final long M_NR = TelephonyManager.NETWORK_TYPE_BITMASK_NR;

    /** Mask không đọc được (lỗi / thiếu quyền). */
    public static final long UNREADABLE = -1L;

    private NetworkTypeMasks() {}

    /** Đổi mask thành chuỗi như "2G 3G LTE 5G"; -1 -> "err", 0 -> "none". */
    public static String decode(long mask) {
        if (mask == UNREADABLE) return "err";
        if (mask == 0) return "none";
        StringBuilder b = new StringBuilder();
        if ((mask & M_2G) != 0) b.append("2G ");
        if ((mask & M_3G) != 0) b.append("3G ");
        if ((mask & M_LTE) != 0) b.append("LTE ");
        if ((mask & M_NR) != 0) b.append("5G ");
        return b.length() == 0 ? "other" : b.toString().trim();
    }

    /** @return true nếu mask có bit 5G (NR) */
    public static boolean hasNr(long mask) {
        return (mask & M_NR) != 0;
    }

    /** @return true nếu mask có ít nhất một bit 2G (GSM, GPRS, EDGE) */
    public static boolean has2g(long mask) {
        return (mask & M_2G) != 0;
    }

    /** Tên công nghệ từ NETWORK_TYPE_* (getDataNetworkType). */
    public static String ratName(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "none";
            default:
                return String.valueOf(networkType);
        }
    }
}
