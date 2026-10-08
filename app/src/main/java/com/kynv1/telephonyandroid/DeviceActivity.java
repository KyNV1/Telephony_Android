package com.kynv1.telephonyandroid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.kynv1.telephonyandroid.biometric.BiometricFlow;
import com.kynv1.telephonyandroid.biometric.BiometricProbe;
import com.kynv1.telephonyandroid.device.DeviceIdProbe;
import com.kynv1.telephonyandroid.device.LocationProbe;
import com.kynv1.telephonyandroid.device.PermissionStatusProbe;
import com.kynv1.telephonyandroid.device.UsbPermissionProbe;
import com.kynv1.telephonyandroid.device.UsbProbe;
import com.kynv1.telephonyandroid.telephony.Permissions;

/**
 * Màn hình thứ hai: thiết bị và sinh trắc. Dùng chung {@link ProbeController} với màn hình Telephony.
 */
public class DeviceActivity extends AppCompatActivity {

    private static final String TAG = "DeviceProbe";

    private ProbeController controller;
    private BiometricFlow biometricFlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ProbeController.setupEdgeToEdge(this, R.id.main);

        TextView reportText = findViewById(R.id.reportText);
        controller = new ProbeController(this, reportText, TAG, savedInstanceState);
        biometricFlow = new BiometricFlow(this, controller);

        controller.bind(R.id.btnDeviceId, DeviceIdProbe::run);
        controller.bind(R.id.btnPermissions, ctx -> PermissionStatusProbe.run(this));
        controller.bind(R.id.btnLocation, LocationProbe::run,
                Permissions.LOCATION, Permissions.LOCATION_COARSE);
        controller.bind(R.id.btnUsb, UsbProbe::run);
        controller.bind(R.id.btnUsbPermission, UsbPermissionProbe::run);
        controller.bind(R.id.btnBiometricAvailability, BiometricProbe::run);

        // Luồng sinh trắc là bất đồng bộ và tự cập nhật báo cáo, nên không đi qua controller.bind.
        findViewById(R.id.btnBiometricTry).setOnClickListener(v -> biometricFlow.start());
        findViewById(R.id.btnResetCounter).setOnClickListener(v -> biometricFlow.resetCounter());
        findViewById(R.id.btnAppSettings).setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null))));

        // Mặc định tắt để vẫn chụp được ảnh màn hình; bật thì ảnh chụp và ảnh xem trước ở màn hình đa nhiệm bị chặn.
        MaterialSwitch secure = findViewById(R.id.switchSecure);
        secure.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        controller.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        controller.destroy();
        super.onDestroy();
    }
}
