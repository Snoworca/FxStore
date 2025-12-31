package com.snoworca.fxstore.integration;

import com.snoworca.fxstore.api.CommitMode;
import com.snoworca.fxstore.api.FxOptions;
import com.snoworca.fxstore.api.FxStore;
import com.snoworca.fxstore.api.OnClosePolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import java.io.File;
import java.nio.file.Path;

/**
 * 통합 테스트 기반 클래스
 *
 * <p>SOLID 준수:
 * <ul>
 *   <li>SRP: 테스트 설정/정리만 담당</li>
 *   <li>OCP: 서브클래스에서 확장 가능</li>
 *   <li>DIP: FxStore 인터페이스에 의존</li>
 * </ul>
 *
 * <p>사용법:
 * <pre>{@code
 * public class MyIntegrationTest extends IntegrationTestBase {
 *     @Test
 *     public void testSomething() throws Exception {
 *         openStore();
 *         NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
 *         // ... 테스트 로직
 *     }
 * }
 * }</pre>
 */
public abstract class IntegrationTestBase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(300);  // 5분 제한

    protected File storeFile;
    protected FxStore store;

    @Before
    public void setUpBase() throws Exception {
        storeFile = tempFolder.newFile("test.fx");
        storeFile.delete();  // FxStore가 새로 생성
    }

    @After
    public void tearDownBase() {
        closeStore();
    }

    /**
     * 기본 옵션으로 Store 열기 (BATCH 모드 사용)
     */
    protected void openStore() throws Exception {
        FxOptions options = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();
        store = FxStore.open(storeFile.toPath(), options);
    }

    /**
     * 사용자 지정 옵션으로 Store 열기
     *
     * @param options FxOptions
     */
    protected void openStore(FxOptions options) throws Exception {
        store = FxStore.open(storeFile.toPath(), options);
    }

    /**
     * Store 닫기 (에러 무시)
     */
    protected void closeStore() {
        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                // ignore
            }
            store = null;
        }
    }

    /**
     * Store 재시작 (닫고 다시 열기)
     */
    protected void reopenStore() throws Exception {
        closeStore();
        openStore();
    }

    /**
     * 사용자 지정 옵션으로 Store 재시작
     *
     * @param options FxOptions
     */
    protected void reopenStore(FxOptions options) throws Exception {
        closeStore();
        openStore(options);
    }

    /**
     * Store 파일 경로 반환
     *
     * @return Path
     */
    protected Path storePath() {
        return storeFile.toPath();
    }

    /**
     * 새 임시 파일 생성 (compactTo 대상 등)
     *
     * @param name 파일명
     * @return File
     */
    protected File newTempFile(String name) throws Exception {
        File file = tempFolder.newFile(name);
        file.delete();  // FxStore가 새로 생성
        return file;
    }

    /**
     * ROLLBACK 정책으로 Store 생성
     * BATCH 모드와 함께 사용해야 rollback이 의미가 있음
     *
     * @return FxOptions Builder
     */
    protected FxOptions.Builder rollbackPolicyBuilder() {
        return FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK);
    }
}
