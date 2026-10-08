package com.kynv1.telephonyandroid;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kynv1.telephonyandroid.device.PermissionHistory;
import com.kynv1.telephonyandroid.telephony.Permissions;
import com.kynv1.telephonyandroid.telephony.ProbeReport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Logic dùng chung cho các màn hình "bấm nút, đọc báo cáo": gắn nút với probe, xin quyền còn thiếu,
 * chạy probe trên thread nền, hiển thị kết quả nếu Activity còn sống và giữ lại text khi xoay màn hình.
 * Phải được tạo trong onCreate (registerForActivityResult cần được gọi trước khi Activity STARTED).
 */
public final class ProbeController {

    public interface Probe {
        ProbeReport run(Context ctx);
    }

    private static final String STATE_REPORT = "report";

    private final AppCompatActivity activity;
    private final String logTag;
    private final TextView reportText;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String[]> permissionLauncher;
    private Probe pendingProbe;

    public ProbeController(AppCompatActivity activity, TextView reportText, String logTag, Bundle savedState) {
        this.activity = activity;
        this.reportText = reportText;
        this.logTag = logTag;
        if (savedState != null) {
            String saved = savedState.getString(STATE_REPORT);
            if (saved != null) reportText.setText(saved);
        }
        // Xin quyền xong thì chạy probe đang chờ dù được cấp hay không: probe tự báo thiếu quyền.
        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    PermissionHistory.markAsked(activity, result.keySet());
                    Probe probe = pendingProbe;
                    pendingProbe = null;
                    if (probe != null) run(probe);
                });
    }

    /** Bật edge-to-edge và đệm nội dung theo thanh hệ thống. */
    public static void setupEdgeToEdge(AppCompatActivity activity, int rootId) {
        EdgeToEdge.enable(activity);
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(rootId), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void bind(int buttonId, Probe probe, String... permissions) {
        activity.findViewById(buttonId).setOnClickListener(v -> {
            List<String> missing = new ArrayList<>();
            for (String p : permissions) {
                if (!Permissions.has(activity, p)) missing.add(p);
            }
            if (missing.isEmpty()) {
                run(probe);
            } else {
                pendingProbe = probe;
                permissionLauncher.launch(missing.toArray(new String[0]));
            }
        });
    }

    /** Chạy probe trên thread nền. Gọi từ main thread. */
    public void run(Probe probe) {
        reportText.setText(R.string.report_running);
        Context appContext = activity.getApplicationContext();
        executor.execute(() -> {
            ProbeReport report;
            try {
                report = probe.run(appContext);
            } catch (Throwable t) {
                report = new ProbeReport().err("Lỗi không mong đợi: " + ProbeReport.rootCause(t));
            }
            show(report);
        });
    }

    /**
     * Hiển thị một báo cáo. An toàn khi gọi từ thread bất kỳ; bỏ qua nếu Activity đã đóng.
     * Báo cáo đánh dấu nhạy cảm sẽ không được ghi vào logcat.
     */
    public void show(ProbeReport report) {
        if (!report.isSensitive()) report.log(logTag);
        String text = report.toText();
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            reportText.setText(text);
        });
    }

    public void saveState(Bundle outState) {
        outState.putString(STATE_REPORT, reportText.getText().toString());
    }

    public void destroy() {
        executor.shutdownNow();
    }
}
