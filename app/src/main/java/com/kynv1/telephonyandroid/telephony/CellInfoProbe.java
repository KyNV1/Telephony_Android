package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import java.util.List;

/**
 * Phase 5: CellId và vùng (LAC/TAC) của các cell xung quanh. Cần ACCESS_FINE_LOCATION.
 * 2G/3G dùng LAC, LTE/5G dùng TAC.
 */
public final class CellInfoProbe {

    private CellInfoProbe() {}

    /**
     * Điểm vào của probe cell: kiểm tra quyền vị trí rồi in danh sách cell xung quanh của từng SIM.
     *
     * @param ctx context của ứng dụng
     * @return báo cáo các cell theo từng SIM, hoặc lỗi thiếu quyền vị trí; không bao giờ null
     */
    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        if (!Permissions.has(ctx, Permissions.LOCATION)) {
            r.err(Permissions.missingMessage(Permissions.LOCATION));
            return r;
        }
        SubscriptionScanner.forEach(ctx, r, (si, tm) -> describe(r, tm));
        return r;
    }

    /**
     * Lấy danh sách cell của một SIM và in từng cell; cell đang kết nối được đánh dấu.
     * Danh sách rỗng hoặc thiếu quyền được ghi kèm lý do.
     *
     * @param r  báo cáo để ghi kết quả
     * @param tm TelephonyManager đã gắn đúng SIM này
     */
    @SuppressLint("MissingPermission")
    private static void describe(ProbeReport r, TelephonyManager tm) {
        List<CellInfo> cells;
        try {
            cells = tm.getAllCellInfo();
        } catch (SecurityException e) {
            r.err("Không đọc được cell: " + ProbeReport.rootCause(e));
            return;
        }
        if (cells == null || cells.isEmpty()) {
            r.warn("Không có thông tin cell (GPS/vị trí đang tắt, chưa có sóng, hoặc hệ thống chưa cập nhật)");
            return;
        }
        for (CellInfo ci : cells) {
            String tag = ci.isRegistered() ? "[đang kết nối] " : "";
            r.info(tag + describeCell(ci));
        }
    }

    /**
     * Mô tả một cell theo công nghệ: LTE (CI, TAC), 3G và 2G (CID, LAC), 5G (NCI, TAC).
     * Loại cell khác chỉ in tên lớp.
     *
     * @param ci cell cần mô tả
     */
    private static String describeCell(CellInfo ci) {
        if (ci instanceof CellInfoLte) {
            CellIdentityLte id = ((CellInfoLte) ci).getCellIdentity();
            return "LTE  CI=" + CellValues.format(id.getCi()) + " TAC=" + CellValues.format(id.getTac());
        }
        if (ci instanceof CellInfoWcdma) {
            CellIdentityWcdma id = ((CellInfoWcdma) ci).getCellIdentity();
            return "3G   CID=" + CellValues.format(id.getCid()) + " LAC=" + CellValues.format(id.getLac());
        }
        if (ci instanceof CellInfoGsm) {
            CellIdentityGsm id = ((CellInfoGsm) ci).getCellIdentity();
            return "2G   CID=" + CellValues.format(id.getCid()) + " LAC=" + CellValues.format(id.getLac());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ci instanceof CellInfoNr) {
            return describeNr((CellInfoNr) ci);
        }
        return "Loại cell khác: " + ci.getClass().getSimpleName();
    }

    /** Mô tả cell 5G (NCI, TAC); tách riêng vì CellInfoNr chỉ có từ API 29. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static String describeNr(CellInfoNr ci) {
        CellIdentityNr id = (CellIdentityNr) ci.getCellIdentity();
        return "5G   NCI=" + CellValues.format(id.getNci()) + " TAC=" + CellValues.format(id.getTac());
    }
}
