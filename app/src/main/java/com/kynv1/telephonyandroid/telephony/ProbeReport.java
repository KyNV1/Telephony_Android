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
    private boolean sensitive;

    /** Đánh dấu báo cáo chứa dữ liệu nhạy cảm (vị trí, ID máy): người hiển thị không được ghi log. */
    public ProbeReport sensitive() {
        sensitive = true;
        return this;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public ProbeReport section(String title) {
        return add(Level.SECTION, title);
    }

    public ProbeReport info(String text) {
        return add(Level.INFO, text);
    }

    public ProbeReport ok(String text) {
        return add(Level.OK, text);
    }

    public ProbeReport warn(String text) {
        return add(Level.WARN, text);
    }

    public ProbeReport err(String text) {
        return add(Level.ERR, text);
    }

    /** Bỏ qua một bước nhưng luôn kèm lý do, không bỏ qua im lặng. */
    public ProbeReport skip(String text) {
        return add(Level.SKIP, text);
    }

    private ProbeReport add(Level level, String text) {
        lines.add(new Line(level, text));
        return this;
    }

    public int count(Level level) {
        int n = 0;
        for (Line l : lines) {
            if (l.level == level) n++;
        }
        return n;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

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

    public void log(String tag) {
        Log.i(tag, toText());
    }

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
