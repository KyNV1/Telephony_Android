package com.kynv1.telephonyandroid.biometric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.biometric.BiometricManager;

import org.junit.Test;

public class BiometricAvailabilityTest {

    @Test
    public void mapsEveryKnownCode() {
        assertEquals(BiometricAvailability.READY,
                BiometricAvailability.fromCode(BiometricManager.BIOMETRIC_SUCCESS));
        assertEquals(BiometricAvailability.NONE_ENROLLED,
                BiometricAvailability.fromCode(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED));
        assertEquals(BiometricAvailability.NO_HARDWARE,
                BiometricAvailability.fromCode(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE));
        assertEquals(BiometricAvailability.HARDWARE_UNAVAILABLE,
                BiometricAvailability.fromCode(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE));
        assertEquals(BiometricAvailability.SECURITY_UPDATE_REQUIRED,
                BiometricAvailability.fromCode(BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED));
        assertEquals(BiometricAvailability.UNSUPPORTED,
                BiometricAvailability.fromCode(BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED));
    }

    @Test
    public void unknownCodesFallBackToUnknown() {
        assertEquals(BiometricAvailability.UNKNOWN, BiometricAvailability.fromCode(BiometricManager.BIOMETRIC_STATUS_UNKNOWN));
        assertEquals(BiometricAvailability.UNKNOWN, BiometricAvailability.fromCode(12345));
    }

    @Test
    public void onlyReadyIsReady() {
        assertTrue(BiometricAvailability.READY.isReady());
        for (BiometricAvailability a : BiometricAvailability.values()) {
            if (a != BiometricAvailability.READY) assertFalse(a.name(), a.isReady());
        }
    }

    @Test
    public void describe_appendsHintOnlyWhenPresent() {
        assertEquals("Sẵn sàng", BiometricAvailability.READY.describe());
        assertTrue(BiometricAvailability.NONE_ENROLLED.describe().startsWith("Chưa đăng ký: "));
    }
}
