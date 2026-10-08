package com.kynv1.telephonyandroid.biometric;

import android.content.Context;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;

import com.kynv1.telephonyandroid.telephony.ProbeReport;

/** Phase 5: máy có thể dùng những loại xác thực nào, ở hạng nào. */
public final class BiometricProbe {

    private BiometricProbe() {}

    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        BiometricManager bm = BiometricManager.from(ctx);
        r.section("Khả năng xác thực của máy");
        report(r, bm, "Sinh trắc mạnh (vân tay, một số khuôn mặt)", Authenticators.BIOMETRIC_STRONG);
        report(r, bm, "Sinh trắc yếu trở lên (gồm nhiều loại mở khóa khuôn mặt)", Authenticators.BIOMETRIC_WEAK);
        report(r, bm, "PIN / mẫu hình / mật khẩu của máy", Authenticators.DEVICE_CREDENTIAL);
        r.info("App chỉ nhận kết quả đúng/sai; không bao giờ nhận được dữ liệu vân tay hay khuôn mặt.");
        r.info("Hạng mạnh yêu cầu độ an toàn cao hơn; nhiều máy xếp mở khóa khuôn mặt vào hạng yếu.");
        return r;
    }

    private static void report(ProbeReport r, BiometricManager bm, String label, int authenticators) {
        BiometricAvailability a;
        try {
            a = BiometricAvailability.fromCode(bm.canAuthenticate(authenticators));
        } catch (RuntimeException e) {
            r.err(label + ": " + ProbeReport.rootCause(e));
            return;
        }
        String line = label + ": " + a.describe();
        if (a.isReady()) {
            r.ok(line);
        } else if (a == BiometricAvailability.NONE_ENROLLED || a == BiometricAvailability.HARDWARE_UNAVAILABLE) {
            r.warn(line);
        } else {
            r.skip(line);
        }
    }
}
