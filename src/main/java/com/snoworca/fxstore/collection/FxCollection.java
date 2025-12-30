package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.core.FxStoreImpl;

/**
 * FxStore 컬렉션의 내부 인터페이스
 *
 * <p>모든 FxStore 컬렉션 구현체가 구현하는 내부 인터페이스입니다.
 * 읽기 트랜잭션 등 내부 연산에서 컬렉션 메타데이터에 접근하는 데 사용됩니다.</p>
 *
 * <p><b>주의:</b> 이 인터페이스는 내부 API입니다.
 * 외부 코드에서 직접 사용하면 안 됩니다.</p>
 *
 * @since 0.6
 */
public interface FxCollection {

    /**
     * 컬렉션 ID 반환
     *
     * <p>Store 내에서 이 컬렉션을 식별하는 고유 ID입니다.</p>
     *
     * @return 컬렉션 ID
     */
    long getCollectionId();

    /**
     * 이 컬렉션을 소유한 FxStoreImpl 반환
     *
     * @return FxStoreImpl 인스턴스
     */
    FxStoreImpl getStore();
}
