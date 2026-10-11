package com.kynv1.telephonyandroid;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kynv1.telephonyandroid.databinding.ActivityMainBinding;
import com.kynv1.telephonyandroid.telephony.CarrierConfigProbe;
import com.kynv1.telephonyandroid.telephony.CellInfoProbe;
import com.kynv1.telephonyandroid.telephony.FiveGProbe;
import com.kynv1.telephonyandroid.telephony.Permissions;
import com.kynv1.telephonyandroid.telephony.ProbeReport;
import com.kynv1.telephonyandroid.telephony.RatProbe;
import com.kynv1.telephonyandroid.telephony.SimInfoProbe;
import com.kynv1.telephonyandroid.telephony.VolteProbe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TelephonyProbe";
    private static final String STATE_REPORT = "report";

    /** Một lần đọc dữ liệu telephony; chạy trên thread nền và luôn trả về báo cáo. */
    private interface Probe {
        ProbeReport run(Context ctx);
    }

    // Probe chạy trên một thread nền; kết quả được đẩy về UI nếu Activity còn sống.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityMainBinding binding;
    private Probe pendingProbe;
    private ActivityResultLauncher<String[]> permissionLauncher;

    /**
     * Dựng giao diện bằng ViewBinding, xử lý inset edge-to-edge, khôi phục báo cáo đã lưu
     * khi Activity bị tạo lại (xoay màn hình), đăng ký bộ xin quyền và gắn sáu nút với probe tương ứng.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString(STATE_REPORT);
            if (saved != null) binding.reportText.setText(saved);
        }

        // Xin quyền xong thì chạy probe đang chờ dù được cấp hay không: probe tự báo thiếu quyền.
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Probe probe = pendingProbe;
                    pendingProbe = null;
                    if (probe != null) run(probe);
                });

        bind(binding.btnSim, SimInfoProbe::run, Permissions.PHONE);
        bind(binding.btnRat, RatProbe::run, Permissions.PHONE);
        bind(binding.btnFiveG, FiveGProbe::run, Permissions.PHONE);
        bind(binding.btnConfig, CarrierConfigProbe::run, Permissions.PHONE);
        bind(binding.btnVolte, VolteProbe::run, Permissions.PHONE);
        bind(binding.btnCell, CellInfoProbe::run, Permissions.PHONE, Permissions.LOCATION, Permissions.LOCATION_COARSE);
    }

    /**
     * Gắn một nút với một probe. Khi bấm: đủ quyền thì chạy ngay, thiếu quyền thì lưu probe vào
     * {@code pendingProbe} rồi xin các quyền còn thiếu.
     *
     * @param button      nút kích hoạt probe
     * @param probe       probe cần chạy
     * @param permissions các quyền runtime probe cần
     */
    private void bind(View button, Probe probe, String... permissions) {
        button.setOnClickListener(v -> {
            List<String> missing = new ArrayList<>();
            for (String p : permissions) {
                if (!Permissions.has(this, p)) missing.add(p);
            }
            if (missing.isEmpty()) {
                run(probe);
            } else {
                pendingProbe = probe;
                permissionLauncher.launch(missing.toArray(new String[0]));
            }
        });
    }

    /**
     * Hiện trạng thái "đang đọc", chạy probe trên thread nền rồi đẩy báo cáo về UI.
     * Exception bất ngờ được đổi thành một dòng lỗi trong báo cáo; nếu Activity đã đóng thì bỏ kết quả.
     *
     * @param probe probe cần chạy
     */
    private void run(Probe probe) {
        binding.reportText.setText(R.string.report_running);
        Context appContext = getApplicationContext();
        executor.execute(() -> {
            ProbeReport report;
            try {
                report = probe.run(appContext);
            } catch (Throwable t) {
                report = new ProbeReport().err("Lỗi không mong đợi: " + ProbeReport.rootCause(t));
            }
            report.log(TAG);
            String text = report.toText();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                binding.reportText.setText(text);
            });
        });
    }

    /** Lưu nội dung báo cáo đang hiển thị để {@link #onCreate} khôi phục sau khi Activity bị tạo lại. */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_REPORT, binding.reportText.getText().toString());
    }

    /** Dừng thread nền để không còn probe nào chạy sau khi Activity đã đóng. */
    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
