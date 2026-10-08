package com.kynv1.telephonyandroid.device;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PermissionStateTest {

    @Test
    public void granted_winsOverEverything() {
        assertEquals(PermissionState.GRANTED, PermissionState.of(true, false, false));
        assertEquals(PermissionState.GRANTED, PermissionState.of(true, true, true));
    }

    @Test
    public void neverAsked_isNotAsked() {
        assertEquals(PermissionState.NOT_ASKED, PermissionState.of(false, false, false));
    }

    @Test
    public void deniedOnce_canAskAgain() {
        assertEquals(PermissionState.DENIED_CAN_ASK, PermissionState.of(false, true, true));
    }

    @Test
    public void askedButNoRationale_isPermanentlyDenied() {
        assertEquals(PermissionState.DENIED_PERMANENTLY, PermissionState.of(false, false, true));
    }

    @Test
    public void rationaleWithoutHistory_stillCanAsk() {
        assertEquals(PermissionState.DENIED_CAN_ASK, PermissionState.of(false, true, false));
    }
}
