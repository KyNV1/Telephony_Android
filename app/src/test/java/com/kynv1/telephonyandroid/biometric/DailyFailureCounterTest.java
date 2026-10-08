package com.kynv1.telephonyandroid.biometric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class DailyFailureCounterTest {

    private static final String DAY1 = "2026-10-09";
    private static final String DAY2 = "2026-10-10";

    private static final class MemoryStore implements DailyFailureCounter.Store {
        String day;
        int count;

        @Override
        public String loadDay() {
            return day;
        }

        @Override
        public int loadCount() {
            return count;
        }

        @Override
        public void save(String day, int count) {
            this.day = day;
            this.count = count;
        }

        @Override
        public void clear() {
            day = null;
            count = 0;
        }
    }

    private MemoryStore store;
    private DailyFailureCounter counter;

    @Before
    public void setUp() {
        store = new MemoryStore();
        counter = new DailyFailureCounter(store, 3);
    }

    @Test
    public void startsAtZeroAndUnlocked() {
        assertEquals(0, counter.count(DAY1));
        assertFalse(counter.isLocked(DAY1));
        assertEquals(3, counter.remaining(DAY1));
    }

    @Test
    public void recordFailure_countsUp() {
        assertEquals(1, counter.recordFailure(DAY1));
        assertEquals(2, counter.recordFailure(DAY1));
        assertEquals(2, counter.count(DAY1));
        assertEquals(1, counter.remaining(DAY1));
    }

    @Test
    public void locksAtLimit() {
        counter.recordFailure(DAY1);
        counter.recordFailure(DAY1);
        assertFalse(counter.isLocked(DAY1));
        counter.recordFailure(DAY1);
        assertTrue(counter.isLocked(DAY1));
        assertEquals(0, counter.remaining(DAY1));
    }

    @Test
    public void staysLockedPastLimit() {
        for (int i = 0; i < 5; i++) counter.recordFailure(DAY1);
        assertTrue(counter.isLocked(DAY1));
        assertEquals(0, counter.remaining(DAY1));
    }

    @Test
    public void newDay_resetsAutomatically() {
        for (int i = 0; i < 3; i++) counter.recordFailure(DAY1);
        assertTrue(counter.isLocked(DAY1));
        assertFalse(counter.isLocked(DAY2));
        assertEquals(0, counter.count(DAY2));
        assertEquals(1, counter.recordFailure(DAY2));
        assertEquals(DAY2, store.day);
    }

    @Test
    public void reset_clearsCount() {
        counter.recordFailure(DAY1);
        counter.recordFailure(DAY1);
        counter.reset();
        assertEquals(0, counter.count(DAY1));
        assertFalse(counter.isLocked(DAY1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void limitMustBePositive() {
        new DailyFailureCounter(store, 0);
    }
}
