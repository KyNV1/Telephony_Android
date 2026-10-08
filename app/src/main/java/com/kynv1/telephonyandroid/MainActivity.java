package com.kynv1.telephonyandroid;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.kynv1.telephonyandroid.telephony.CarrierConfigProbe;
import com.kynv1.telephonyandroid.telephony.CellInfoProbe;
import com.kynv1.telephonyandroid.telephony.FiveGProbe;
import com.kynv1.telephonyandroid.telephony.Permissions;
import com.kynv1.telephonyandroid.telephony.RatProbe;
import com.kynv1.telephonyandroid.telephony.SimInfoProbe;
import com.kynv1.telephonyandroid.telephony.VolteProbe;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TelephonyProbe";

    private ProbeController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ProbeController.setupEdgeToEdge(this, R.id.main);

        controller = new ProbeController(this, findViewById(R.id.reportText), TAG, savedInstanceState);
        controller.bind(R.id.btnSim, SimInfoProbe::run, Permissions.PHONE);
        controller.bind(R.id.btnRat, RatProbe::run, Permissions.PHONE);
        controller.bind(R.id.btnFiveG, FiveGProbe::run, Permissions.PHONE);
        controller.bind(R.id.btnConfig, CarrierConfigProbe::run, Permissions.PHONE);
        controller.bind(R.id.btnVolte, VolteProbe::run, Permissions.PHONE);
        controller.bind(R.id.btnCell, CellInfoProbe::run,
                Permissions.PHONE, Permissions.LOCATION, Permissions.LOCATION_COARSE);

        findViewById(R.id.btnDevice).setOnClickListener(
                v -> startActivity(new Intent(this, DeviceActivity.class)));
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
