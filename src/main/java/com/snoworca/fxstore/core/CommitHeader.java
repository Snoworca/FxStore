package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import com.snoworca.fxstore.util.CRC32C;

/**
 * CommitHeader - 커밋 상태를 나타내는 헤더
 * 
 * 파일 레이아웃:
 * - Offset 4096: Slot A (4096 bytes)
 * - Offset 8192: Slot B (4096 bytes)
 * 
 * 두 슬롯 중 seqNo가 큰 유효한 헤더를 선택하여 최신 커밋 상태 복구.
 * 
 * 바이트 레이아웃 (4096 bytes):
 * [0-7]    : magic "FXHDR\0\0\0"
 * [8-11]   : headerVersion (u32 LE)
 * [12-15]  : _padding1
 * [16-23]  : seqNo (u64 LE) - 단조 증가
 * [24-31]  : committedFlags (u64 LE) - bit 0: SYNC durability
 * [32-39]  : allocTail (u64 LE) - 다음 할당 오프셋
 * [40-47]  : catalogRootPageId (u64 LE)
 * [48-55]  : stateRootPageId (u64 LE)
 * [56-63]  : nextCollectionId (u64 LE)
 * [64-71]  : commitEpochMs (u64 LE) - 커밋 시각
 * [72-4091]: reserved (0x00)
 * [4092-4095]: CRC32C (u32 LE)
 */
public class CommitHeader {
    
    public static final int SIZE = 4096;
    public static final byte[] MAGIC = new byte[] {
        0x46, 0x58, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00  // "FXHDR\0\0\0"
    };
    public static final int CURRENT_VERSION = 1;
    
    private final long seqNo;
    private final long committedFlags;
    private final long allocTail;
    private final long catalogRootPageId;
    private final long stateRootPageId;
    private final long nextCollectionId;
    private final long commitEpochMs;
    
    public CommitHeader(
        long seqNo,
        long committedFlags,
        long allocTail,
        long catalogRootPageId,
        long stateRootPageId,
        long nextCollectionId,
        long commitEpochMs
    ) {
        this.seqNo = seqNo;
        this.committedFlags = committedFlags;
        this.allocTail = allocTail;
        this.catalogRootPageId = catalogRootPageId;
        this.stateRootPageId = stateRootPageId;
        this.nextCollectionId = nextCollectionId;
        this.commitEpochMs = commitEpochMs;
    }
    
    /**
     * 바이트 배열로 인코딩
     */
    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(SIZE);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        // Magic
        buf.put(MAGIC);
        
        // HeaderVersion
        buf.putInt(CURRENT_VERSION);
        
        // Padding1
        buf.putInt(0);
        
        // SeqNo
        buf.putLong(seqNo);
        
        // CommittedFlags
        buf.putLong(committedFlags);
        
        // AllocTail
        buf.putLong(allocTail);
        
        // CatalogRootPageId
        buf.putLong(catalogRootPageId);
        
        // StateRootPageId
        buf.putLong(stateRootPageId);
        
        // NextCollectionId
        buf.putLong(nextCollectionId);
        
        // CommitEpochMs
        buf.putLong(commitEpochMs);
        
        // Reserved (72 ~ 4091 = 4020 bytes)
        byte[] reserved = new byte[4020];
        Arrays.fill(reserved, (byte) 0);
        buf.put(reserved);
        
        // CRC32C 계산 (0 ~ 4091)
        byte[] data = buf.array();
        int crcValue = CRC32C.compute(data, 0, 4092);
        
        // CRC 기록
        buf.putInt(crcValue);
        
        return buf.array();
    }
    
    /**
     * 바이트 배열에서 디코딩
     */
    public static CommitHeader decode(byte[] data) {
        if (data.length < SIZE) {
            throw new FxException(
                FxErrorCode.CORRUPTION,
                "CommitHeader size must be " + SIZE + " bytes, got " + data.length
            );
        }
        
        ByteBuffer buf = ByteBuffer.wrap(data, 0, SIZE);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        // Magic 검증
        byte[] magic = new byte[8];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new FxException(
                FxErrorCode.CORRUPTION,
                "Invalid CommitHeader magic"
            );
        }
        
        // HeaderVersion
        int version = buf.getInt();
        if (version != CURRENT_VERSION) {
            throw new FxException(
                FxErrorCode.VERSION_MISMATCH,
                "Unsupported CommitHeader version: " + version
            );
        }
        
        // Padding1
        buf.getInt();
        
        // 필드 읽기
        long seqNo = buf.getLong();
        long committedFlags = buf.getLong();
        long allocTail = buf.getLong();
        long catalogRootPageId = buf.getLong();
        long stateRootPageId = buf.getLong();
        long nextCollectionId = buf.getLong();
        long commitEpochMs = buf.getLong();
        
        return new CommitHeader(
            seqNo,
            committedFlags,
            allocTail,
            catalogRootPageId,
            stateRootPageId,
            nextCollectionId,
            commitEpochMs
        );
    }
    
    /**
     * CRC 검증
     */
    public boolean verify(byte[] data) {
        if (data.length < SIZE) {
            return false;
        }
        
        // 저장된 CRC
        ByteBuffer buf = ByteBuffer.wrap(data, 4092, 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int storedCrc = buf.getInt();
        
        // 계산된 CRC
        int computedCrc = CRC32C.compute(data, 0, 4092);
        
        return storedCrc == computedCrc;
    }
    
    /**
     * 두 슬롯 중 유효한 최신 헤더 선택
     * 
     * @param slotA Slot A 데이터 (4096 bytes)
     * @param slotB Slot B 데이터 (4096 bytes)
     * @return 선택된 헤더
     * @throws FxException 두 슬롯 모두 손상된 경우
     */
    public static CommitHeader selectHeader(byte[] slotA, byte[] slotB) {
        CommitHeader headerA = null;
        CommitHeader headerB = null;
        boolean aValid = false;
        boolean bValid = false;
        
        // Slot A 검증
        try {
            headerA = decode(slotA);
            aValid = headerA.verify(slotA);
        } catch (Exception e) {
            aValid = false;
        }
        
        // Slot B 검증
        try {
            headerB = decode(slotB);
            bValid = headerB.verify(slotB);
        } catch (Exception e) {
            bValid = false;
        }
        
        // 두 슬롯 모두 손상
        if (!aValid && !bValid) {
            throw new FxException(
                FxErrorCode.CORRUPTION,
                "Both CommitHeader slots are corrupted"
            );
        }
        
        // 하나만 유효
        if (!aValid) {
            return headerB;
        }
        if (!bValid) {
            return headerA;
        }
        
        // 둘 다 유효 → seqNo가 큰 것 선택
        return headerA.seqNo >= headerB.seqNo ? headerA : headerB;
    }
    
    // Getters
    public long getSeqNo() {
        return seqNo;
    }
    
    public long getCommittedFlags() {
        return committedFlags;
    }
    
    public long getAllocTail() {
        return allocTail;
    }
    
    public long getCatalogRootPageId() {
        return catalogRootPageId;
    }
    
    public long getStateRootPageId() {
        return stateRootPageId;
    }
    
    public long getNextCollectionId() {
        return nextCollectionId;
    }
    
    public long getCommitEpochMs() {
        return commitEpochMs;
    }
    
    @Override
    public String toString() {
        return String.format(
            "CommitHeader{seqNo=%d, allocTail=%d, catalogRoot=%d, stateRoot=%d, nextColId=%d, epoch=%d}",
            seqNo, allocTail, catalogRootPageId, stateRootPageId, nextCollectionId, commitEpochMs
        );
    }
}
