package com.snoworca.fxstore.integration.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 파일 손상 시뮬레이션 유틸리티
 *
 * <p><b>주의</b>: 테스트 목적으로만 사용. 실제 데이터에는 사용 금지.
 *
 * <p>FxStore 파일 구조:
 * <ul>
 *   <li>Superblock: 0 ~ 4095 (4KB)</li>
 *   <li>CommitHeader Slot A: 4096 ~ 4607 (512B)</li>
 *   <li>CommitHeader Slot B: 4608 ~ 5119 (512B)</li>
 *   <li>Data pages: 5120 ~ ...</li>
 * </ul>
 */
public final class FileCorruptor {

    /** Superblock 크기 (4KB) */
    private static final int SUPERBLOCK_SIZE = 4096;

    /** CommitHeader 크기 (512B) */
    private static final int COMMIT_HEADER_SIZE = 512;

    private FileCorruptor() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * Superblock Magic 바이트 손상
     *
     * <p>FxStore 파일의 첫 번째 바이트를 손상시켜 Magic 검증 실패를 유발합니다.
     *
     * @param file 손상시킬 파일
     * @throws IOException 파일 접근 오류 시
     */
    public static void corruptSuperblockMagic(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(0);
            raf.writeByte(0xFF);  // Magic 바이트 손상
        }
    }

    /**
     * Superblock CRC 손상
     *
     * <p>Superblock의 CRC 필드를 손상시켜 무결성 검증 실패를 유발합니다.
     *
     * @param file 손상시킬 파일
     * @throws IOException 파일 접근 오류 시
     */
    public static void corruptSuperblockCRC(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            // CRC는 보통 Superblock 끝 부분에 위치
            raf.seek(SUPERBLOCK_SIZE - 4);
            raf.writeInt(0xDEADBEEF);  // CRC 손상
        }
    }

    /**
     * CommitHeader CRC 손상
     *
     * <p>지정된 슬롯(A=0, B=1)의 CommitHeader CRC를 손상시킵니다.
     *
     * @param file 손상시킬 파일
     * @param slot 슬롯 번호 (0=A, 1=B)
     * @throws IOException 파일 접근 오류 시
     * @throws IllegalArgumentException 잘못된 슬롯 번호
     */
    public static void corruptCommitHeaderCRC(File file, int slot) throws IOException {
        if (slot < 0 || slot > 1) {
            throw new IllegalArgumentException("slot must be 0 or 1, got: " + slot);
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            long offset = SUPERBLOCK_SIZE + (slot * COMMIT_HEADER_SIZE);
            // CRC는 CommitHeader 끝 부분에 위치
            raf.seek(offset + COMMIT_HEADER_SIZE - 4);
            raf.writeInt(0xDEADBEEF);  // CRC 손상
        }
    }

    /**
     * CommitHeader seqNo 손상
     *
     * <p>지정된 슬롯의 시퀀스 번호를 임의의 값으로 변경합니다.
     *
     * @param file 손상시킬 파일
     * @param slot 슬롯 번호 (0=A, 1=B)
     * @param newSeqNo 새 시퀀스 번호
     * @throws IOException 파일 접근 오류 시
     */
    public static void corruptCommitHeaderSeqNo(File file, int slot, long newSeqNo) throws IOException {
        if (slot < 0 || slot > 1) {
            throw new IllegalArgumentException("slot must be 0 or 1, got: " + slot);
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            long offset = SUPERBLOCK_SIZE + (slot * COMMIT_HEADER_SIZE);
            // seqNo는 CommitHeader 앞 부분에 위치한다고 가정
            raf.seek(offset + 8);  // magic(4) + version(4) 다음
            raf.writeLong(newSeqNo);
        }
    }

    /**
     * 파일 트렁케이션 (부분 쓰기 시뮬레이션)
     *
     * <p>파일을 지정된 크기로 잘라 부분 쓰기나 크래시 상황을 시뮬레이션합니다.
     *
     * @param file 손상시킬 파일
     * @param newSize 새 파일 크기 (바이트)
     * @throws IOException 파일 접근 오류 시
     * @throws IllegalArgumentException 음수 크기
     */
    public static void truncateFile(File file, long newSize) throws IOException {
        if (newSize < 0) {
            throw new IllegalArgumentException("newSize cannot be negative: " + newSize);
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(newSize);
        }
    }

    /**
     * 임의의 오프셋에 바이트 쓰기
     *
     * <p>파일의 특정 위치에 임의의 바이트를 써서 손상을 유발합니다.
     *
     * @param file 손상시킬 파일
     * @param offset 쓰기 오프셋
     * @param value 쓸 바이트 값
     * @throws IOException 파일 접근 오류 시
     */
    public static void writeByte(File file, long offset, byte value) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(offset);
            raf.writeByte(value);
        }
    }

    /**
     * 임의의 오프셋에 바이트 배열 쓰기
     *
     * @param file 손상시킬 파일
     * @param offset 쓰기 오프셋
     * @param data 쓸 바이트 배열
     * @throws IOException 파일 접근 오류 시
     */
    public static void writeBytes(File file, long offset, byte[] data) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(offset);
            raf.write(data);
        }
    }

    /**
     * 파일의 특정 범위를 0으로 채우기
     *
     * @param file 손상시킬 파일
     * @param offset 시작 오프셋
     * @param length 채울 길이
     * @throws IOException 파일 접근 오류 시
     */
    public static void zeroFill(File file, long offset, int length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(offset);
            raf.write(new byte[length]);  // 0으로 채움
        }
    }

    /**
     * 데이터 페이지 영역의 임의 위치 손상
     *
     * <p>Superblock과 CommitHeader 이후의 데이터 영역에서 임의의 위치를 손상시킵니다.
     *
     * @param file 손상시킬 파일
     * @param pageOffset 데이터 영역 시작 이후의 오프셋
     * @throws IOException 파일 접근 오류 시
     */
    public static void corruptDataPage(File file, long pageOffset) throws IOException {
        long dataAreaStart = SUPERBLOCK_SIZE + (2 * COMMIT_HEADER_SIZE);  // 5120
        writeByte(file, dataAreaStart + pageOffset, (byte) 0xFF);
    }

    /**
     * 파일 끝에 쓰레기 데이터 추가
     *
     * @param file 손상시킬 파일
     * @param garbageSize 추가할 쓰레기 크기
     * @throws IOException 파일 접근 오류 시
     */
    public static void appendGarbage(File file, int garbageSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(raf.length());
            byte[] garbage = new byte[garbageSize];
            for (int i = 0; i < garbageSize; i++) {
                garbage[i] = (byte) (i % 256);
            }
            raf.write(garbage);
        }
    }
}
