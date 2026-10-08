package com.kynv1.telephonyandroid.device;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DeviceIdProbeTest {

    @Test
    public void format_macWithColonsUppercase() {
        assertEquals("02:00:00:AB:CD:EF",
                DeviceIdProbe.format(new byte[]{0x02, 0x00, 0x00, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF}));
    }

    @Test
    public void format_emptyIsEmpty() {
        assertEquals("", DeviceIdProbe.format(new byte[0]));
    }
}
