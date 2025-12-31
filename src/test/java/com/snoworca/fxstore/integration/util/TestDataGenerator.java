package com.snoworca.fxstore.integration.util;

import java.util.*;

/**
 * 테스트 데이터 생성기
 *
 * <p>SOLID 준수:
 * <ul>
 *   <li>SRP: 테스트 데이터 생성만 담당</li>
 *   <li>OCP: 새 데이터 타입 추가 시 확장 가능</li>
 * </ul>
 *
 * <p>모든 메서드는 재현 가능한 결과를 위해 시드 기반 난수를 사용합니다.
 */
public final class TestDataGenerator {

    private TestDataGenerator() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * 순차적 Long 데이터 생성
     *
     * @param count 생성할 개수
     * @return 0부터 count-1까지의 순차 리스트
     */
    public static List<Long> sequentialLongs(int count) {
        List<Long> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add((long) i);
        }
        return result;
    }

    /**
     * 랜덤 Long 데이터 생성 (재현 가능한 시드)
     *
     * @param count 생성할 개수
     * @param seed 랜덤 시드
     * @return 랜덤 Long 리스트
     */
    public static List<Long> randomLongs(int count, long seed) {
        Random random = new Random(seed);
        List<Long> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(random.nextLong());
        }
        return result;
    }

    /**
     * 랜덤 Long 데이터 생성 (기본 시드 42)
     *
     * @param count 생성할 개수
     * @return 랜덤 Long 리스트
     */
    public static List<Long> randomLongs(int count) {
        return randomLongs(count, 42L);
    }

    /**
     * 역순 Long 데이터 생성
     *
     * @param count 생성할 개수
     * @return count-1부터 0까지의 역순 리스트
     */
    public static List<Long> reverseLongs(int count) {
        List<Long> result = new ArrayList<>(count);
        for (int i = count - 1; i >= 0; i--) {
            result.add((long) i);
        }
        return result;
    }

    /**
     * 셔플된 Long 데이터 생성
     *
     * @param count 생성할 개수
     * @param seed 셔플 시드
     * @return 셔플된 Long 리스트
     */
    public static List<Long> shuffledLongs(int count, long seed) {
        List<Long> result = sequentialLongs(count);
        Collections.shuffle(result, new Random(seed));
        return result;
    }

    /**
     * 셔플된 Long 데이터 생성 (기본 시드 42)
     *
     * @param count 생성할 개수
     * @return 셔플된 Long 리스트
     */
    public static List<Long> shuffledLongs(int count) {
        return shuffledLongs(count, 42L);
    }

    /**
     * 큰 문자열 데이터 생성
     *
     * @param length 문자열 길이
     * @return 알파벳 소문자로 구성된 문자열
     */
    public static String largeString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        return sb.toString();
    }

    /**
     * 랜덤 문자열 생성
     *
     * @param length 문자열 길이
     * @param seed 랜덤 시드
     * @return 랜덤 알파벳 소문자 문자열
     */
    public static String randomString(int length, long seed) {
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    /**
     * 랜덤 문자열 생성 (기본 시드 42)
     *
     * @param length 문자열 길이
     * @return 랜덤 문자열
     */
    public static String randomString(int length) {
        return randomString(length, 42L);
    }

    /**
     * 랜덤 바이트 배열 생성
     *
     * @param length 배열 길이
     * @param seed 랜덤 시드
     * @return 랜덤 바이트 배열
     */
    public static byte[] randomBytes(int length, long seed) {
        Random random = new Random(seed);
        byte[] result = new byte[length];
        random.nextBytes(result);
        return result;
    }

    /**
     * 랜덤 바이트 배열 생성 (기본 시드 42)
     *
     * @param length 배열 길이
     * @return 랜덤 바이트 배열
     */
    public static byte[] randomBytes(int length) {
        return randomBytes(length, 42L);
    }

    /**
     * 순차적 문자열 맵 생성
     *
     * @param count 생성할 개수
     * @return Long -> "value{index}" 맵
     */
    public static Map<Long, String> sequentialMap(int count) {
        Map<Long, String> map = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            map.put((long) i, "value" + i);
        }
        return map;
    }

    /**
     * 유니코드/특수문자 테스트용 문자열 배열
     *
     * @return 테스트용 특수 문자열 배열
     */
    public static String[] specialStrings() {
        return new String[] {
                "한글키",
                "日本語",
                "emoji\uD83D\uDE00",
                "tab\there",
                "newline\nhere",
                "quote\"here",
                "backslash\\here",
                "null\u0000char",
                "",  // 빈 문자열
                "   ",  // 공백만
                "very-long-key-" + largeString(100)
        };
    }

    /**
     * 경계 테스트용 Long 값들
     *
     * @return 경계값 배열
     */
    public static Long[] boundaryLongs() {
        return new Long[] {
                Long.MIN_VALUE,
                Long.MIN_VALUE + 1,
                -1L,
                0L,
                1L,
                Long.MAX_VALUE - 1,
                Long.MAX_VALUE
        };
    }
}
