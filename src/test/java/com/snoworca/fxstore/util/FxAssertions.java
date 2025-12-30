package com.snoworca.fxstore.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * 커스텀 어설션 유틸리티
 */
public class FxAssertions {

    public static <K, V> void assertMapEquals(Map<K, V> expected, NavigableMap<K, V> actual) {
        assertEquals("Size mismatch", expected.size(), actual.size());
        for (Map.Entry<K, V> entry : expected.entrySet()) {
            assertEquals("Value mismatch for key " + entry.getKey(),
                    entry.getValue(), actual.get(entry.getKey()));
        }
    }

    public static <E> void assertSetEquals(Set<E> expected, NavigableSet<E> actual) {
        assertEquals("Size mismatch", expected.size(), actual.size());
        for (E element : expected) {
            assertTrue("Missing element: " + element, actual.contains(element));
        }
    }

    public static void assertOrderPreserved(NavigableSet<Long> set) {
        Long prev = null;
        for (Long current : set) {
            if (prev != null) {
                assertTrue("Order violation: " + prev + " >= " + current, prev < current);
            }
            prev = current;
        }
    }

    public static <K extends Comparable<K>> void assertMapOrderPreserved(NavigableMap<K, ?> map) {
        K prev = null;
        for (K current : map.keySet()) {
            if (prev != null) {
                assertTrue("Order violation: " + prev + " >= " + current, prev.compareTo(current) < 0);
            }
            prev = current;
        }
    }

    public static void assertBytesEqual(byte[] expected, byte[] actual) {
        assertNotNull("Expected array is null", expected);
        assertNotNull("Actual array is null", actual);
        assertEquals("Array length mismatch", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Byte mismatch at index " + i, expected[i], actual[i]);
        }
    }
}
