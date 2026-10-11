package com.kynv1.telephonyandroid.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

/**
 * Phase 3: gom các tín hiệu về 5G rồi nhờ {@link FiveGDiagnoser} kết luận.
 * Tín hiệu công khai (RAT, carrier config, TelephonyDisplayInfo) luôn dùng được; mask và NR state
 * có thể không đọc được và khi đó bước tương ứng được bỏ qua kèm lý do.
 */
@SuppressLint("InlinedApi")
public final class FiveGProbe {

    private FiveGProbe() {}

    /** NR state đọc bằng reflection vì getNrState/getNetworkRegistrationInfo là API ẩn. */
    private static final class NrState {
        final int state;
        final String error;

        NrState(int state, String error) {
            this.state = state;
            this.error = error;
        }
    }

    /**
     * Điểm vào của probe 5G: với mỗi SIM đang hoạt động, in các tín hiệu 5G và kết luận chẩn đoán.
     * Phải gọi từ thread nền vì {@link DisplayInfoReader} chờ callback.
     *
     * @param ctx context của ứng dụng
     * @return báo cáo chẩn đoán 5G theo từng SIM, không bao giờ null
     */
    public static ProbeReport run(Context ctx) {
        ProbeReport r = new ProbeReport();
        SubscriptionScanner.forEach(ctx, r, (si, tm) -> diagnose(ctx, r, si, tm));
        return r;
    }

    /**
     * Thu thập RAT, NR state, mask USER/CARRIER, carrier config và display override của một SIM,
     * in ra từng tín hiệu (bước không đọc được thì ghi lý do bỏ qua) rồi ghi kết luận
     * của {@link FiveGDiagnoser}: mức OK nếu đang 5G, ngược lại là cảnh báo.
     *
     * @param ctx context của ứng dụng
     * @param r   báo cáo để ghi kết quả
     * @param si  thông tin subscription của SIM đang xét
     * @param tm  TelephonyManager đã gắn đúng SIM này
     */
    private static void diagnose(Context ctx, ProbeReport r, SubscriptionInfo si, TelephonyManager tm) {
        int rat = RatProbe.dataNetworkType(tm);
        NrState nr = readNrState(tm);
        RatProbe.Mask user = RatProbe.readMask(tm, RatProbe.REASON_USER);
        RatProbe.Mask carrier = RatProbe.readMask(tm, RatProbe.REASON_CARRIER);
        String cfg = CarrierConfigProbe.nrConfigText(ctx, si.getSubscriptionId());
        DisplayInfoReader.Result display = DisplayInfoReader.read(ctx, tm);

        r.info("RAT dữ liệu: " + NetworkTypeMasks.ratName(rat));
        r.info("Biểu tượng mạng (display override): " + FiveGDiagnoser.overrideName(display.overrideType));
        r.info("NR state: " + FiveGDiagnoser.nrName(nr.state));
        r.info("Carrier config cho phép NR: " + cfg);
        r.info("Mask USER có NR: " + describe(user.hasNr()) + " | Mask CARRIER có NR: " + describe(carrier.hasNr()));

        if (display.error != null) r.skip("Display override: " + display.error);
        if (nr.error != null) r.skip("NR state: " + nr.error);
        if (!user.ok() || !carrier.ok()) {
            r.skip("Bỏ qua kiểm tra mask USER/CARRIER: " + (user.ok() ? carrier.error : user.error));
        }

        FiveGDiagnoser.Verdict v = FiveGDiagnoser.verdict(
                user.hasNr(), carrier.hasNr(), cfg, rat, nr.state, display.overrideType);
        if (v == FiveGDiagnoser.Verdict.ON_5G) {
            r.ok("Kết luận: " + v.description);
        } else {
            r.warn("Kết luận: " + v.description);
        }
    }

    /** Đổi kết quả có thể null thành chữ: "true", "false" hoặc "không rõ" khi không đọc được. */
    private static String describe(Boolean value) {
        return value == null ? "không rõ" : String.valueOf(value);
    }

    /**
     * Đọc NR state của đăng ký dữ liệu (PS, WWAN) bằng reflection vì đây là API ẩn. Không đọc được
     * (API dưới 30, ServiceState null, API ẩn bị chặn...) thì trả {@link FiveGDiagnoser#NR_UNREADABLE}
     * kèm lý do, không ném lỗi.
     *
     * @param tm TelephonyManager đã gắn đúng SIM cần đọc
     * @return NR state hoặc lý do không đọc được
     */
    private static NrState readNrState(TelephonyManager tm) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return new NrState(FiveGDiagnoser.NR_UNREADABLE, "cần API 30 trở lên");
        }
        try {
            @SuppressLint("MissingPermission")
            ServiceState ss = tm.getServiceState();
            if (ss == null) return new NrState(FiveGDiagnoser.NR_UNREADABLE, "ServiceState null");
            Object nri = ServiceState.class
                    .getMethod("getNetworkRegistrationInfo", int.class, int.class)
                    .invoke(ss, NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri == null) return new NrState(FiveGDiagnoser.NR_UNREADABLE, "NetworkRegistrationInfo null");
            Object st = nri.getClass().getMethod("getNrState").invoke(nri);
            if (st instanceof Integer) return new NrState((Integer) st, null);
            return new NrState(FiveGDiagnoser.NR_UNREADABLE, "kiểu trả về không mong đợi");
        } catch (Throwable t) {
            // API ẩn có thể bị chặn (NoSuchMethodException) hoặc thiếu quyền: không phải lỗi của app.
            return new NrState(FiveGDiagnoser.NR_UNREADABLE, ProbeReport.rootCause(t));
        }
    }
}
