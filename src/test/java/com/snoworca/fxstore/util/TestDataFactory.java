package com.snoworca.fxstore.util;

import java.util.*;

/**
 * 테스트 데이터 생성 유틸리티
 */
public class TestDataFactory {

    private static final Random RANDOM = new Random(42);

    public static Map<Long, String> createLongStringMap(int size) {
        Map<Long, String> map = new HashMap<>();
        for (long i = 0; i < size; i++) {
            map.put(i, "value" + i);
        }
        return map;
    }

    public static List<Long> createRandomLongList(int size) {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(RANDOM.nextLong());
        }
        return list;
    }

    public static Set<Long> createSequentialLongSet(int size) {
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < size; i++) {
            set.add(i);
        }
        return set;
    }

    public static byte[] createRandomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static List<Long> createShuffledLongList(int size) {
        List<Long> list = new ArrayList<>();
        for (long i = 0; i < size; i++) {
            list.add(i);
        }
        Collections.shuffle(list, new Random(42));
        return list;
    }

    public static String createRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + RANDOM.nextInt(26)));
        }
        return sb.toString();
    }
}
