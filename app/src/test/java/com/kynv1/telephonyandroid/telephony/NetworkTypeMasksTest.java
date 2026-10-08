package com.kynv1.telephonyandroid.telephony;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.telephony.TelephonyManager;

import org.junit.Test;

public class NetworkTypeMasksTest {

    @Test
    public void decode_unreadableIsErr() {
        assertEquals("err", NetworkTypeMasks.decode(NetworkTypeMasks.UNREADABLE));
    }

    @Test
    public void decode_zeroIsNone() {
        assertEquals("none", NetworkTypeMasks.decode(0));
    }

    @Test
    public void decode_combinesGenerationsInOrder() {
        assertEquals("2G LTE", NetworkTypeMasks.decode(NetworkTypeMasks.M_2G | NetworkTypeMasks.M_LTE));
        assertEquals("LTE 5G", NetworkTypeMasks.decode(NetworkTypeMasks.M_LTE | NetworkTypeMasks.M_NR));
        assertEquals("2G 3G LTE 5G", NetworkTypeMasks.decode(
                NetworkTypeMasks.M_2G | NetworkTypeMasks.M_3G | NetworkTypeMasks.M_LTE | NetworkTypeMasks.M_NR));
    }

    @Test
    public void decode_singleBitInsideAGroupStillShowsGroup() {
        assertEquals("2G", NetworkTypeMasks.decode(TelephonyManager.NETWORK_TYPE_BITMASK_EDGE));
    }

    @Test
    public void decode_bitOutsideKnownGroupsIsOther() {
        assertEquals("other", NetworkTypeMasks.decode(TelephonyManager.NETWORK_TYPE_BITMASK_CDMA));
    }

    @Test
    public void hasNrAndHas2g() {
        assertTrue(NetworkTypeMasks.hasNr(NetworkTypeMasks.M_LTE | NetworkTypeMasks.M_NR));
        assertFalse(NetworkTypeMasks.hasNr(NetworkTypeMasks.M_LTE));
        assertTrue(NetworkTypeMasks.has2g(NetworkTypeMasks.M_2G));
        assertFalse(NetworkTypeMasks.has2g(NetworkTypeMasks.M_3G));
    }

    @Test
    public void ratName_mapsGenerations() {
        assertEquals("5G", NetworkTypeMasks.ratName(TelephonyManager.NETWORK_TYPE_NR));
        assertEquals("LTE", NetworkTypeMasks.ratName(TelephonyManager.NETWORK_TYPE_LTE));
        assertEquals("3G", NetworkTypeMasks.ratName(TelephonyManager.NETWORK_TYPE_HSPAP));
        assertEquals("2G", NetworkTypeMasks.ratName(TelephonyManager.NETWORK_TYPE_EDGE));
        assertEquals("none", NetworkTypeMasks.ratName(TelephonyManager.NETWORK_TYPE_UNKNOWN));
        assertEquals("4", NetworkTypeMasks.ratName(TelephonyManager.NETWORK_TYPE_CDMA));
    }
}
