package com.kynv1.telephonyandroid.device;

import java.util.Locale;

/** Tên lớp thiết bị/giao diện USB (theo chuẩn USB-IF) để báo cáo dễ đọc. */
public final class UsbClasses {

    private UsbClasses() {}

    public static String name(int usbClass) {
        switch (usbClass) {
            case 0x00: return "theo giao diện";
            case 0x01: return "âm thanh";
            case 0x02: return "giao tiếp (CDC)";
            case 0x03: return "HID (bàn phím, chuột...)";
            case 0x06: return "ảnh (PTP)";
            case 0x07: return "máy in";
            case 0x08: return "lưu trữ (USB ổ đĩa)";
            case 0x09: return "hub";
            case 0x0B: return "thẻ thông minh";
            case 0x0E: return "video";
            case 0xE0: return "không dây";
            case 0xFF: return "đặc thù nhà sản xuất";
            default:   return "lớp 0x" + Integer.toHexString(usbClass).toUpperCase(Locale.ROOT);
        }
    }
}
