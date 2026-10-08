package com.kynv1.telephonyandroid.telephony;

import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.CFG_ERR;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.CFG_NA;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.CFG_NONE;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.NR_CONNECTED;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.NR_NONE;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.NR_NOT_RESTRICTED;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.NR_RESTRICTED;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.NR_UNREADABLE;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.OVERRIDE_UNKNOWN;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.Verdict;
import static com.kynv1.telephonyandroid.telephony.FiveGDiagnoser.verdict;
import static org.junit.Assert.assertEquals;

import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;

import org.junit.Test;

public class FiveGDiagnoserTest {

    private static final int LTE = TelephonyManager.NETWORK_TYPE_LTE;
    private static final int UMTS = TelephonyManager.NETWORK_TYPE_UMTS;
    private static final int UNKNOWN = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private static final String NSA_SA = "NSA+SA";

    @Test
    public void connectedNr_isOn5g() {
        assertEquals(Verdict.ON_5G, verdict(true, true, NSA_SA, LTE, NR_CONNECTED, OVERRIDE_UNKNOWN));
    }

    @Test
    public void dataTypeNr_isOn5g_evenWithoutNrState() {
        assertEquals(Verdict.ON_5G,
                verdict(null, null, CFG_NA, TelephonyManager.NETWORK_TYPE_NR, NR_UNREADABLE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void displayInfoNrNsa_isOn5g_whenOnlyPublicSignalAvailable() {
        assertEquals(Verdict.ON_5G, verdict(null, null, CFG_NA, LTE, NR_UNREADABLE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA));
    }

    @Test
    public void userMaskWithoutNr() {
        assertEquals(Verdict.USER_MASK_NO_NR, verdict(false, true, NSA_SA, LTE, NR_NONE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void userMaskIsCheckedBeforeCarrierMask() {
        assertEquals(Verdict.USER_MASK_NO_NR, verdict(false, false, NSA_SA, LTE, NR_NONE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void carrierMaskWithoutNr() {
        assertEquals(Verdict.CARRIER_MASK_NO_NR, verdict(true, false, NSA_SA, LTE, NR_NONE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void carrierConfigWithoutNr() {
        assertEquals(Verdict.CONFIG_NO_NR, verdict(true, true, CFG_NONE, LTE, NR_NONE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void rat3g_meansNoLteAnchor() {
        assertEquals(Verdict.NO_LTE_ANCHOR, verdict(true, true, NSA_SA, UMTS, NR_NONE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void lteWithRestrictedNr() {
        assertEquals(Verdict.NR_RESTRICTED_BY_CELL, verdict(true, true, NSA_SA, LTE, NR_RESTRICTED, OVERRIDE_UNKNOWN));
    }

    @Test
    public void lteWithUnreadableNrState() {
        assertEquals(Verdict.NR_STATE_UNREADABLE, verdict(true, true, NSA_SA, LTE, NR_UNREADABLE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void lteWithNrNone_isCoverage() {
        assertEquals(Verdict.NO_NR_COVERAGE, verdict(true, true, NSA_SA, LTE, NR_NONE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void lteWithNrNotRestricted_isCoverage() {
        assertEquals(Verdict.NO_NR_COVERAGE, verdict(true, true, NSA_SA, LTE, NR_NOT_RESTRICTED, OVERRIDE_UNKNOWN));
    }

    @Test
    public void unknownMasks_areNotTreatedAsMissingNr() {
        // Ứng dụng thường không đọc được mask: không được kết luận "mask thiếu NR".
        assertEquals(Verdict.NR_STATE_UNREADABLE,
                verdict(null, null, NSA_SA, LTE, NR_UNREADABLE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void unreadableConfig_doesNotBlockLaterChecks() {
        assertEquals(Verdict.NO_NR_COVERAGE, verdict(true, true, CFG_ERR, LTE, NR_NONE, OVERRIDE_UNKNOWN));
    }

    @Test
    public void formatNrAvailabilities() {
        assertEquals(CFG_NONE, FiveGDiagnoser.formatNrAvailabilities(null));
        assertEquals(CFG_NONE, FiveGDiagnoser.formatNrAvailabilities(new int[0]));
        assertEquals("NSA", FiveGDiagnoser.formatNrAvailabilities(
                new int[]{CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA}));
        assertEquals("NSA+SA", FiveGDiagnoser.formatNrAvailabilities(
                new int[]{CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA}));
        assertEquals("SA+9", FiveGDiagnoser.formatNrAvailabilities(
                new int[]{CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA, 9}));
    }

    @Test
    public void nrNames() {
        assertEquals("NONE", FiveGDiagnoser.nrName(NR_NONE));
        assertEquals("CONNECTED", FiveGDiagnoser.nrName(NR_CONNECTED));
        assertEquals("n/a", FiveGDiagnoser.nrName(NR_UNREADABLE));
    }
}
