package com.kynv1.telephonyandroid.device;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UsbClassesTest {

    @Test
    public void knownClasses() {
        assertEquals("HID (bàn phím, chuột...)", UsbClasses.name(0x03));
        assertEquals("lưu trữ (USB ổ đĩa)", UsbClasses.name(0x08));
        assertEquals("hub", UsbClasses.name(0x09));
        assertEquals("đặc thù nhà sản xuất", UsbClasses.name(0xFF));
        assertEquals("theo giao diện", UsbClasses.name(0));
    }

    @Test
    public void unknownClassShowsHex() {
        assertEquals("lớp 0x5A", UsbClasses.name(0x5A));
    }
}
