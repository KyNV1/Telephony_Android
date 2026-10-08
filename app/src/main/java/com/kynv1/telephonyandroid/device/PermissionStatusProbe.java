package com.kynv1.telephonyandroid.device;

import android.app.Activity;

import androidx.core.app.ActivityCompat;

import com.kynv1.telephonyandroid.telephony.Permissions;
import com.kynv1.telephonyandroid.telephony.ProbeReport;

/** Phase 2: trạng thái từng quyền runtime mà app dùng. */
public final class PermissionStatusProbe {

    private static final String[] TRACKED = {
            Permissions.PHONE, Permissions.LOCATION, Permissions.LOCATION_COARSE,
    };

    private PermissionStatusProbe() {}

    public static ProbeReport run(Activity activity) {
        ProbeReport r = new ProbeReport();
        r.section("Trạng thái quyền");
        boolean needSettings = false;
        for (String p : TRACKED) {
            PermissionState state = PermissionState.of(
                    Permissions.has(activity, p),
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, p),
                    PermissionHistory.wasAsked(activity, p));
            String line = Permissions.shortName(p) + ": " + state.label;
            if (state == PermissionState.GRANTED) {
                r.ok(line);
            } else if (state == PermissionState.DENIED_PERMANENTLY) {
                r.warn(line);
                needSettings = true;
            } else {
                r.info(line);
            }
        }
        if (needSettings) {
            r.info("Quyền bị từ chối vĩnh viễn không xin lại được bằng hộp thoại: bấm 'Mở cài đặt' để bật.");
        }
        return r;
    }
}
