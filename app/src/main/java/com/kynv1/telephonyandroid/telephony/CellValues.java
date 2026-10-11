package com.kynv1.telephonyandroid.telephony;

/**
 * Đổi giá trị định danh cell sang chữ. Hệ thống dùng Integer.MAX_VALUE / Long.MAX_VALUE
 * (CellInfo.UNAVAILABLE) và -1 để báo "không có"; các giá trị này không được hiển thị như số thật.
 */
public final class CellValues {

    public static final String UNAVAILABLE = "n/a";

    private CellValues() {}

    /** Đổi giá trị int thành chữ; Integer.MAX_VALUE và -1 thành {@link #UNAVAILABLE}. */
    public static String format(int value) {
        return (value == Integer.MAX_VALUE || value == -1) ? UNAVAILABLE : String.valueOf(value);
    }

    /** Đổi giá trị long thành chữ; Long.MAX_VALUE và -1 thành {@link #UNAVAILABLE}. */
    public static String format(long value) {
        return (value == Long.MAX_VALUE || value == -1L) ? UNAVAILABLE : String.valueOf(value);
    }
}
