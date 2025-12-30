package com.snoworca.fxstore.util;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * FxStore 테스트를 위한 공통 베이스 클래스
 */
public abstract class FxStoreTestBase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected FxStore memoryStore;
    protected FxStore fileStore;
    protected File tempFile;

    @Before
    public void setUpBase() throws Exception {
        memoryStore = FxStore.openMemory();
        tempFile = tempFolder.newFile("test.fx");
        tempFile.delete();
        fileStore = FxStore.open(tempFile.toPath());
    }

    @After
    public void tearDownBase() {
        closeQuietly(memoryStore);
        closeQuietly(fileStore);
    }

    protected void closeQuietly(FxStore store) {
        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    protected <T> void assertRoundTrip(FxCodec<T> codec, T value) {
        byte[] encoded = codec.encode(value);
        T decoded = codec.decode(encoded);
        assertEquals(value, decoded);
    }

    protected int compareBytes(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (cmp != 0) return cmp;
        }
        return a.length - b.length;
    }
}
