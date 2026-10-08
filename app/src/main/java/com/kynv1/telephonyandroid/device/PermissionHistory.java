package com.kynv1.telephonyandroid.device;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;

/** Ghi nhớ quyền nào app đã từng hỏi, để phân biệt "chưa hỏi" với "từ chối vĩnh viễn". */
public final class PermissionHistory {

    private static final String PREFS = "permission_history";

    private PermissionHistory() {}

    public static void markAsked(Context ctx, Collection<String> permissions) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        for (String p : permissions) editor.putBoolean(p, true);
        editor.apply();
    }

    public static boolean wasAsked(Context ctx, String permission) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(permission, false);
    }
}
