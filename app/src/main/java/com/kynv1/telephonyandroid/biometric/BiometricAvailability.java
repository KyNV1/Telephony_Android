package com.kynv1.telephonyandroid.biometric;

import androidx.biometric.BiometricManager;

/**
 * Kết quả {@code BiometricManager.canAuthenticate} đổi sang trạng thái dễ hiểu kèm hướng dẫn.
 * Logic thuần: nhận mã số, không gọi dịch vụ hệ thống.
 */
public enum BiometricAvailability {
    READY("Sẵn sàng", ""),
    NONE_ENROLLED("Chưa đăng ký", "Vào Cài đặt > Sinh trắc học (hoặc Bảo mật) để đăng ký vân tay/khuôn mặt"),
    NO_HARDWARE("Máy không có phần cứng phù hợp", ""),
    HARDWARE_UNAVAILABLE("Phần cứng tạm thời không dùng được", "Thử lại sau"),
    SECURITY_UPDATE_REQUIRED("Cần cập nhật bảo mật", "Cập nhật hệ điều hành rồi thử lại"),
    UNSUPPORTED("Không được hỗ trợ ở phiên bản Android này", ""),
    UNKNOWN("Không xác định", "");

    public final String description;
    public final String hint;

    BiometricAvailability(String description, String hint) {
        this.description = description;
        this.hint = hint;
    }

    public boolean isReady() {
        return this == READY;
    }

    public static BiometricAvailability fromCode(int code) {
        switch (code) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return READY;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return NONE_ENROLLED;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return NO_HARDWARE;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return HARDWARE_UNAVAILABLE;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                return SECURITY_UPDATE_REQUIRED;
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                return UNSUPPORTED;
            default:
                return UNKNOWN;
        }
    }

    /** "Mô tả" hoặc "Mô tả: hướng dẫn" nếu có hướng dẫn. */
    public String describe() {
        return hint.isEmpty() ? description : description + ": " + hint;
    }
}
