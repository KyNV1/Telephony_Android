package com.kynv1.telephonyandroid.biometric;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.kynv1.telephonyandroid.ProbeController;
import com.kynv1.telephonyandroid.telephony.ProbeReport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Phase 6: luồng xác thực nhân viên mô phỏng theo app Bitel gốc, dùng sinh trắc của chính điện thoại.
 * Kiểm tra khả dụng -> thử sinh trắc mạnh -> mỗi lần không khớp cộng vào bộ đếm trong ngày ->
 * đủ ngưỡng thì hủy sinh trắc và chuyển sang PIN/mẫu hình của máy.
 * App chỉ nhận đúng/sai, không nhận dữ liệu sinh trắc, và chỉ lưu số lần thất bại.
 * Phải tạo và gọi từ main thread.
 */
public final class BiometricFlow {

    public static final int MAX_FAILURES_PER_DAY = 3;
    private static final long FALLBACK_DELAY_MS = 500;

    private final AppCompatActivity activity;
    private final ProbeController controller;
    private final DailyFailureCounter counter;
    private final Executor mainExecutor;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ProbeReport log = new ProbeReport();
    private BiometricPrompt activePrompt;
    private boolean switchingToFallback;

    public BiometricFlow(AppCompatActivity activity, ProbeController controller) {
        this.activity = activity;
        this.controller = controller;
        this.counter = new DailyFailureCounter(new PrefsFailureStore(activity), MAX_FAILURES_PER_DAY);
        this.mainExecutor = ContextCompat.getMainExecutor(activity);
    }

    public void start() {
        log = new ProbeReport();
        switchingToFallback = false;
        String today = today();
        log.section("Thử xác thực sinh trắc");
        log.info("Thất bại hôm nay: " + counter.count(today) + "/" + MAX_FAILURES_PER_DAY);

        if (counter.isLocked(today)) {
            log.warn("Đã khóa sinh trắc hôm nay (đủ ngưỡng thất bại): chuyển sang PIN/mẫu hình của máy");
            startCredential();
            return;
        }
        BiometricManager bm = BiometricManager.from(activity);
        BiometricAvailability strong = BiometricAvailability.fromCode(bm.canAuthenticate(Authenticators.BIOMETRIC_STRONG));
        if (!strong.isReady()) {
            log.skip("Không dùng được sinh trắc mạnh: " + strong.describe());
            log.info("Thử phương án dự phòng: PIN/mẫu hình của máy");
            startCredential();
            return;
        }
        startBiometric();
    }

    /** Xóa bộ đếm để thử lại luồng khóa từ đầu (chỉ phục vụ học và kiểm thử). */
    public void resetCounter() {
        counter.reset();
        log = new ProbeReport();
        log.ok("Đã đặt lại bộ đếm thất bại hôm nay: 0/" + MAX_FAILURES_PER_DAY);
        controller.show(log);
    }

    private void startBiometric() {
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực nhân viên")
                .setSubtitle("Dùng vân tay (hoặc khuôn mặt hạng mạnh)")
                .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Hủy")
                .build();
        activePrompt = new BiometricPrompt(activity, mainExecutor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                counter.reset();
                log.ok("Xác thực sinh trắc thành công (đã xóa bộ đếm thất bại)");
                controller.show(log);
            }

            @Override
            public void onAuthenticationFailed() {
                onBiometricMismatch();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                onBiometricError(errorCode, errString);
            }
        });
        log.info("Đã mở hộp thoại sinh trắc, hãy đặt ngón tay lên cảm biến...");
        controller.show(log);
        activePrompt.authenticate(info);
    }

    private void onBiometricMismatch() {
        String today = today();
        int n = counter.recordFailure(today);
        log.warn("Không khớp: lần " + n + "/" + MAX_FAILURES_PER_DAY + " hôm nay");
        if (counter.isLocked(today)) {
            log.warn("Đủ ngưỡng: hủy sinh trắc và chuyển sang phương án dự phòng");
            switchingToFallback = true;
            activePrompt.cancelAuthentication();
            // Thường hệ thống báo ERROR_CANCELED để mở bước dự phòng; hẹn giờ phòng khi không có callback.
            handler.postDelayed(this::startCredentialOnce, FALLBACK_DELAY_MS);
        }
        controller.show(log);
    }

    private void onBiometricError(int errorCode, CharSequence message) {
        if (switchingToFallback && errorCode == BiometricPrompt.ERROR_CANCELED) {
            startCredentialOnce();
            return;
        }
        switch (errorCode) {
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                log.info("Người dùng đã hủy");
                controller.show(log);
                break;
            case BiometricPrompt.ERROR_LOCKOUT:
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                log.warn("Cảm biến bị hệ thống tạm khóa vì thử sai nhiều lần (" + message + "): chuyển sang PIN/mẫu hình");
                startCredential();
                break;
            default:
                log.err("Lỗi sinh trắc " + errorCode + ": " + message);
                controller.show(log);
        }
    }

    private void startCredentialOnce() {
        if (!switchingToFallback) return;
        switchingToFallback = false;
        handler.removeCallbacksAndMessages(null);
        startCredential();
    }

    private void startCredential() {
        BiometricManager bm = BiometricManager.from(activity);
        BiometricAvailability credential = BiometricAvailability.fromCode(
                bm.canAuthenticate(Authenticators.DEVICE_CREDENTIAL));
        if (!credential.isReady()) {
            log.skip("Không có phương án dự phòng: máy chưa đặt khóa màn hình (" + credential.describe() + ")");
            controller.show(log);
            return;
        }
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực dự phòng")
                .setSubtitle("Nhập PIN, mẫu hình hoặc mật khẩu của máy")
                .setAllowedAuthenticators(Authenticators.DEVICE_CREDENTIAL)
                .build();
        BiometricPrompt prompt = new BiometricPrompt(activity, mainExecutor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                log.ok("Xác thực dự phòng bằng PIN/mẫu hình thành công");
                controller.show(log);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    log.info("Người dùng đã hủy bước dự phòng");
                } else {
                    log.err("Lỗi xác thực dự phòng " + errorCode + ": " + errString);
                }
                controller.show(log);
            }
        });
        log.info("Đã mở hộp thoại PIN/mẫu hình...");
        controller.show(log);
        prompt.authenticate(info);
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
