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

    private interface Probe {
        ProbeReport run(Context ctx);
    }

    // Probe chạy trên một thread nền; kết quả được đẩy về UI nếu Activity còn sống.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView reportText;
    private Probe pendingProbe;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reportText = findViewById(R.id.reportText);
        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString(STATE_REPORT);
            if (saved != null) reportText.setText(saved);
        }

        // Xin quyền xong thì chạy probe đang chờ dù được cấp hay không: probe tự báo thiếu quyền.
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Probe probe = pendingProbe;
                    pendingProbe = null;
                    if (probe != null) run(probe);
                });

        bind(R.id.btnSim, SimInfoProbe::run, Permissions.PHONE);
        bind(R.id.btnRat, RatProbe::run, Permissions.PHONE);
        bind(R.id.btnFiveG, FiveGProbe::run, Permissions.PHONE);
        bind(R.id.btnConfig, CarrierConfigProbe::run, Permissions.PHONE);
        bind(R.id.btnVolte, VolteProbe::run, Permissions.PHONE);
        bind(R.id.btnCell, CellInfoProbe::run, Permissions.PHONE, Permissions.LOCATION, Permissions.LOCATION_COARSE);
    }

    private void bind(int buttonId, Probe probe, String... permissions) {
        findViewById(buttonId).setOnClickListener(v -> {
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

    private void run(Probe probe) {
        reportText.setText(R.string.report_running);
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
                reportText.setText(text);
            });
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_REPORT, reportText.getText().toString());
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
