package com.kynv1.telephonyandroid.telephony;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CellValuesTest {

    @Test
    public void intUnavailableValuesAreHidden() {
        assertEquals("n/a", CellValues.format(Integer.MAX_VALUE));
        assertEquals("n/a", CellValues.format(-1));
    }

    @Test
    public void intRealValuesAreKept() {
        assertEquals("0", CellValues.format(0));
        assertEquals("12345", CellValues.format(12345));
    }

    @Test
    public void longUnavailableValuesAreHidden() {
        assertEquals("n/a", CellValues.format(Long.MAX_VALUE));
        assertEquals("n/a", CellValues.format(-1L));
    }

    @Test
    public void longRealValuesAreKept() {
        assertEquals("68719476735", CellValues.format(68719476735L));
    }
}
