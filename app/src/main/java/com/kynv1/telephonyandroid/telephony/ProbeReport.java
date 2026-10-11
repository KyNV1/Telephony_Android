package com.kynv1.telephonyandroid.telephony;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả của một lần "probe": danh sách dòng có mức độ (OK / cảnh báo / lỗi / bỏ qua).
 * Mọi probe trả về lớp này thay vì chuỗi thô, và dùng chung {@link #rootCause(Throwable)}.
 */
public final class ProbeReport {

    public enum Level { SECTION, INFO, OK, WARN, ERR, SKIP }

    private static final class Line {
        final Level level;
        final String text;

        Line(Level level, String text) {
            this.level = level;
            this.text = text;
        }
    }

    private final List<Line> lines = new ArrayList<>();

    /** Thêm dòng tiêu đề cho một nhóm kết quả (ví dụ mỗi SIM một nhóm). */
    public ProbeReport section(String title) {
        return add(Level.SECTION, title);
    }

    /** Thêm dòng thông tin trung tính. */
    public ProbeReport info(String text) {
        return add(Level.INFO, text);
    }

    /** Thêm dòng báo kết quả tốt, hiển thị với tiền tố "[OK]". */
    public ProbeReport ok(String text) {
        return add(Level.OK, text);
    }

    /** Thêm dòng cảnh báo (có vấn đề nhưng chưa phải lỗi), hiển thị với tiền tố "[!]". */
    public ProbeReport warn(String text) {
        return add(Level.WARN, text);
    }

    /** Thêm dòng báo lỗi (không đọc được dữ liệu ngoài dự kiến), hiển thị với tiền tố "[X]". */
    public ProbeReport err(String text) {
        return add(Level.ERR, text);
    }

    /** Bỏ qua một bước nhưng luôn kèm lý do, không bỏ qua im lặng. */
    public ProbeReport skip(String text) {
        return add(Level.SKIP, text);
    }

    /** Thêm một dòng vào cuối báo cáo; trả về chính báo cáo để gọi nối tiếp. */
    private ProbeReport add(Level level, String text) {
        lines.add(new Line(level, text));
        return this;
    }

    /** Đếm số dòng thuộc một mức độ, dùng để kiểm tra kết quả trong test. */
    public int count(Level level) {
        int n = 0;
        for (Line l : lines) {
            if (l.level == level) n++;
        }
        return n;
    }

    /** @return true nếu chưa có dòng nào */
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /**
     * Đổi báo cáo thành chuỗi để hiển thị: mỗi dòng một hàng kèm tiền tố theo mức độ,
     * tiêu đề nhóm có dạng "== tiêu đề ==" và cách dòng trống với nhóm trước.
     */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        for (Line l : lines) {
            if (l.level == Level.SECTION) {
                if (sb.length() > 0) sb.append('\n');
                sb.append("== ").append(l.text).append(" ==\n");
            } else {
                sb.append(prefix(l.level)).append(l.text).append('\n');
            }
        }
        return sb.toString();
    }

    /** Ghi toàn bộ báo cáo vào Logcat ở mức INFO với tag cho trước. */
    public void log(String tag) {
        Log.i(tag, toText());
    }

    /** Tiền tố hiển thị trước nội dung dòng; INFO dùng khoảng trắng để thẳng hàng với các dòng khác. */
    private static String prefix(Level level) {
        switch (level) {
            case OK:   return "[OK] ";
            case WARN: return "[!] ";
            case ERR:  return "[X] ";
            case SKIP: return "[-] ";
            default:   return "    ";
        }
    }

    /**
     * Lấy nguyên nhân gốc: reflection luôn bọc lỗi thật trong InvocationTargetException,
     * nên cần bóc ra rồi ghép "Loại: thông điệp".
     */
    public static String rootCause(Throwable t) {
        Throwable c = (t instanceof InvocationTargetException && t.getCause() != null) ? t.getCause() : t;
        return c.getClass().getSimpleName() + ": " + c.getMessage();
    }
}
