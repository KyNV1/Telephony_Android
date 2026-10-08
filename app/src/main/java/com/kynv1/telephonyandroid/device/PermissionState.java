package com.kynv1.telephonyandroid.device;

/**
 * Trạng thái một quyền runtime. Android không tự cho biết "chưa từng hỏi" khác "từ chối vĩnh viễn"
 * (cả hai đều có shouldShowRequestPermissionRationale = false), nên cần thêm thông tin "đã từng hỏi".
 */
public enum PermissionState {
    GRANTED("Đã cấp"),
    NOT_ASKED("Chưa hỏi lần nào"),
    DENIED_CAN_ASK("Đã từ chối (có thể hỏi lại)"),
    DENIED_PERMANENTLY("Từ chối vĩnh viễn (phải bật trong Cài đặt)");

    public final String label;

    PermissionState(String label) {
        this.label = label;
    }

    public static PermissionState of(boolean granted, boolean shouldShowRationale, boolean askedBefore) {
        if (granted) return GRANTED;
        if (shouldShowRationale) return DENIED_CAN_ASK;
        return askedBefore ? DENIED_PERMANENTLY : NOT_ASKED;
    }
}
