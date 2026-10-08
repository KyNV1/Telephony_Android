package com.kynv1.telephonyandroid.biometric;

import android.content.Context;
import android.content.SharedPreferences;

/** Lưu ngày và số lần thất bại trong SharedPreferences. Không lưu bất kỳ dữ liệu sinh trắc nào. */
public final class PrefsFailureStore implements DailyFailureCounter.Store {

    private static final String PREFS = "biometric_failures";
    private static final String KEY_DAY = "day";
    private static final String KEY_COUNT = "count";

    private final SharedPreferences prefs;

    public PrefsFailureStore(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public String loadDay() {
        return prefs.getString(KEY_DAY, null);
    }

    @Override
    public int loadCount() {
        return prefs.getInt(KEY_COUNT, 0);
    }

    @Override
    public void save(String day, int count) {
        prefs.edit().putString(KEY_DAY, day).putInt(KEY_COUNT, count).apply();
    }

    @Override
    public void clear() {
        prefs.edit().clear().apply();
    }
}
