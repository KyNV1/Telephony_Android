package com.kynv1.telephonyandroid.device;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.kynv1.telephonyandroid.telephony.ProbeReport;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Phase 4: liệt kê thiết bị USB đang cắm (qua cáp OTG) và cho biết app đã có quyền truy cập từng
 * thiết bị chưa. Đầu đọc vân tay của app Bitel gốc đi qua đúng cơ chế này.
 */
public final class UsbProbe {

    private UsbProbe() {}

    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        if (!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            r.skip("Máy không hỗ trợ USB host (không dùng được cáp OTG)");
            return r;
        }
        UsbManager um = ctx.getSystemService(UsbManager.class);
        if (um == null) {
            r.err("Không lấy được UsbManager");
            return r;
        }
        Map<String, UsbDevice> devices = um.getDeviceList();
        if (devices.isEmpty()) {
            r.info("Không có thiết bị USB nào đang cắm. Cắm một thiết bị qua cáp OTG (ổ USB, chuột...) rồi bấm lại.");
            return r;
        }
        for (UsbDevice d : devices.values()) {
            r.section("USB " + d.getDeviceName());
            r.info(String.format(Locale.US, "vendor-id=0x%04X  product-id=0x%04X", d.getVendorId(), d.getProductId()));
            r.info("Lớp thiết bị: " + UsbClasses.name(d.getDeviceClass()) + " | lớp giao diện: " + interfaceClasses(d));
            try {
                r.info("Hãng / sản phẩm: " + d.getManufacturerName() + " / " + d.getProductName());
            } catch (SecurityException e) {
                r.skip("Tên hãng/sản phẩm: cần quyền truy cập thiết bị");
            }
            if (um.hasPermission(d)) {
                r.ok("App đã có quyền truy cập thiết bị này");
            } else {
                r.warn("App chưa có quyền truy cập thiết bị này (bấm 'Xin quyền USB')");
            }
        }
        return r;
    }

    private static String interfaceClasses(UsbDevice d) {
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < d.getInterfaceCount(); i++) {
            UsbInterface itf = d.getInterface(i);
            names.add(UsbClasses.name(itf.getInterfaceClass()));
        }
        return names.isEmpty() ? "không có" : String.join(", ", names);
    }
}
