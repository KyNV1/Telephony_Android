package com.kynv1.telephonyandroid.telephony;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

/** Quyền runtime mà các probe cần. */
public final class Permissions {

    public static final String PHONE = Manifest.permission.READ_PHONE_STATE;
    public static final String LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    /** Android 12+ yêu cầu xin kèm COARSE trong cùng một lần với FINE, nếu không yêu cầu bị bỏ qua. */
    public static final String LOCATION_COARSE = Manifest.permission.ACCESS_COARSE_LOCATION;

    private Permissions() {}

    /** @return true nếu quyền runtime đã được cấp cho ứng dụng */
    public static boolean has(Context ctx, String permission) {
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /** Thông báo thiếu quyền, chỉ lấy phần tên cuối (ví dụ READ_PHONE_STATE) và hướng dẫn cách cấp. */
    public static String missingMessage(String permission) {
        return "Thiếu quyền " + permission.substring(permission.lastIndexOf('.') + 1)
                + ": cấp trong Cài đặt > Ứng dụng > Quyền rồi chạy lại";
    }
}
