package com.kynv1.telephonyandroid.telephony;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class ProbeReportTest {

    @Test
    public void toText_prefixesEachLevel() {
        ProbeReport r = new ProbeReport();
        r.section("SIM 1").ok("a").warn("b").err("c").skip("d").info("e");
        assertEquals("== SIM 1 ==\n[OK] a\n[!] b\n[X] c\n[-] d\n    e\n", r.toText());
    }

    @Test
    public void toText_separatesSectionsWithBlankLine() {
        ProbeReport r = new ProbeReport();
        r.section("A").ok("x").section("B").ok("y");
        assertEquals("== A ==\n[OK] x\n\n== B ==\n[OK] y\n", r.toText());
    }

    @Test
    public void count_andIsEmpty() {
        ProbeReport r = new ProbeReport();
        assertTrue(r.isEmpty());
        r.skip("a").skip("b").err("c");
        assertEquals(2, r.count(ProbeReport.Level.SKIP));
        assertEquals(1, r.count(ProbeReport.Level.ERR));
        assertEquals(0, r.count(ProbeReport.Level.OK));
    }

    @Test
    public void rootCause_unwrapsInvocationTargetException() {
        Throwable wrapped = new InvocationTargetException(new SecurityException("need privilege"));
        assertEquals("SecurityException: need privilege", ProbeReport.rootCause(wrapped));
    }

    @Test
    public void rootCause_keepsPlainException() {
        assertEquals("IllegalStateException: boom", ProbeReport.rootCause(new IllegalStateException("boom")));
    }
}
