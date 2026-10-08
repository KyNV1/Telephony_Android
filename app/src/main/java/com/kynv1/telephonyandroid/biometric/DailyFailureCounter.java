package com.kynv1.telephonyandroid.biometric;

/**
 * Đếm số lần xác thực thất bại theo ngày và khóa khi đủ ngưỡng (mô phỏng giới hạn
 * MAX_TIME_VALIDATE_FACE_STAFF_FAILED_ON_DAY của app Bitel gốc). Sang ngày mới thì tự tính lại từ 0.
 * "Ngày" là chuỗi do bên gọi cấp (ví dụ "2026-10-09") nên lớp này test được mà không cần đồng hồ.
 */
public final class DailyFailureCounter {

    /** Nơi lưu ngày và số đếm gần nhất. */
    public interface Store {
        String loadDay();

        int loadCount();

        void save(String day, int count);

        void clear();
    }

    private final Store store;
    private final int limit;

    public DailyFailureCounter(Store store, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit phải > 0");
        this.store = store;
        this.limit = limit;
    }

    public int count(String today) {
        return today.equals(store.loadDay()) ? store.loadCount() : 0;
    }

    /** Ghi nhận một lần thất bại, trả số lần thất bại trong ngày sau khi cộng. */
    public int recordFailure(String today) {
        int n = count(today) + 1;
        store.save(today, n);
        return n;
    }

    public boolean isLocked(String today) {
        return count(today) >= limit;
    }

    public int remaining(String today) {
        return Math.max(0, limit - count(today));
    }

    public int limit() {
        return limit;
    }

    public void reset() {
        store.clear();
    }
}
